package reframecv.repository

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.attachments.AttachmentStatus
import reframecv.attachments.AttachmentStorage
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.attachments.TestAttachmentStorage
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.logging.ApplicationLogger

class VacancySnapshotCleanupTest : WorkspaceDatabaseTest() {
    private fun manager(
        storage: AttachmentStorage,
        logger: ApplicationLogger = ApplicationLogger.None,
    ) = VacancySnapshotManager(database.vacancyDao(), database.importRecordDao(), storage, logger)

    @Test
    fun cleanupCannotRemoveFileBeforeItsTransactionCommits() = runBlocking {
        val copied = CompletableDeferred<Unit>()
        val continueSave = CompletableDeferred<Unit>()
        val files = TestAttachmentStorage()
        val storage = object : AttachmentStorage by files {
            override suspend fun store(selection: SelectedAttachment) =
                files.store(selection).also {
                    copied.complete(Unit)
                    continueSave.await()
                }
        }
        val snapshots = manager(storage)
        val record = vacancy(project())
        withTimeout(10_000.milliseconds) {
            val save = async {
                snapshots.create(
                    record,
                    SnapshotChange.Attach(SelectedAttachment("source", "job.txt")),
                )
            }
            copied.await()
            val cleanup = async { snapshots.cleanup() }
            continueSave.complete(Unit)
            val id = save.await()
            cleanup.await()
            assertEquals(AttachmentStatus.Available, snapshots.snapshot(id)?.status)
            assertEquals(1, files.files.size)
        }
    }

    @Test
    fun failedFileDeletionKeepsDatabaseResultAndLaterCleanupRemovesOrphan() = runBlocking {
        val files = TestAttachmentStorage()
        val failures = mutableListOf<String>()
        val logger = ApplicationLogger { operation, _, _ -> failures += operation }
        val storage = object : AttachmentStorage by files {
            override suspend fun delete(filePath: String): Unit = throw IOException("File is busy")
        }
        val snapshots = manager(storage, logger)
        val id = snapshots.create(
            vacancy(project()),
            SnapshotChange.Attach(SelectedAttachment("source", "job.txt")),
        )
        assertNotNull(snapshots.snapshot(id))
        snapshots.delete(id, 500)
        assertEquals(0, database.importRecordDao().filePaths().size)
        assertEquals(1, files.files.size)
        assertEquals(listOf("Remove unused snapshot file"), failures)
        manager(files).cleanup()
        assertEquals(0, files.files.size)
    }
}
