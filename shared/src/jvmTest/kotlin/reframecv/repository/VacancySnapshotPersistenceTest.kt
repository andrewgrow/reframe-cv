package reframecv.repository

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import reframecv.attachments.AttachmentStatus
import reframecv.attachments.FileAttachmentStorage
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.logging.ApplicationLogger

class VacancySnapshotPersistenceTest : WorkspaceDatabaseTest() {
    private fun manager() = VacancySnapshotManager(
        database.vacancyDao(),
        database.importRecordDao(),
        FileAttachmentStorage(temporaryFolder.root.toPath()),
        ApplicationLogger.None,
    )

    private fun attachment(name: String = "source.txt"): SnapshotChange.Attach {
        val file = temporaryFolder.newFile(name)
        file.writeText("Original source: $name")
        return SnapshotChange.Attach(SelectedAttachment(file.path, name))
    }

    @Test
    fun persistsOriginalSnapshotAndKeepsItWhenVacancyChanges() = runBlocking {
        val snapshots = manager()
        val id = snapshots.create(vacancy(project()), attachment())
        val original = assertNotNull(snapshots.snapshot(id))
        assertEquals("file", original.record.provider)
        assertNull(original.record.rawData)
        assertEquals("source.txt", original.record.fileName)
        val vacancy = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        snapshots.update(vacancy.copy(name = "Edited", url = "changed"), SnapshotChange.Keep)
        assertEquals(original, manager().snapshot(id))
        assertEquals(AttachmentStatus.Available, original.status)
    }

    @Test
    fun rollbackRemovesCopiedFileAndSnapshotRecord() = runBlocking {
        assertFailsWith<IllegalStateException> {
            manager().create(vacancy(999), attachment())
        }
        assertTrue(database.importRecordDao().filePaths().isEmpty())
        Files.list(temporaryFolder.root.toPath().resolve("attachments")).use {
            assertEquals(0L, it.count())
        }
    }

    @Test
    fun replacementAndDetachRemoveOnlyUnusedCopies() = runBlocking {
        val snapshots = manager()
        val projectId = project()
        val id = snapshots.create(vacancy(projectId), attachment())
        val original = assertNotNull(snapshots.snapshot(id)).record
        val shared = database.vacancyDao().create(
            vacancy(projectId).copy(importRecordId = original.id),
        )
        val record = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        snapshots.update(record, attachment("replacement.pdf"))
        val replacement = assertNotNull(snapshots.snapshot(id)).record
        assertTrue(Files.exists(temporaryFolder.root.toPath().resolve(original.filePath!!)))
        snapshots.delete(shared, 300)
        assertTrue(!Files.exists(temporaryFolder.root.toPath().resolve(original.filePath)))
        snapshots.update(record, SnapshotChange.Remove)
        assertNull(snapshots.snapshot(id))
        assertTrue(!Files.exists(temporaryFolder.root.toPath().resolve(replacement.filePath!!)))
        assertTrue(database.importRecordDao().filePaths().isEmpty())
    }

    @Test
    fun projectDeletionPreservesSnapshotAndMissingFilePreservesMetadata() = runBlocking<Unit> {
        val snapshots = manager()
        val projectId = project()
        val id = snapshots.create(vacancy(projectId), attachment())
        val original = assertNotNull(snapshots.snapshot(id)).record
        database.projectDao().markSubtreeDeleted(projectId, 400)
        manager().cleanup()
        assertEquals(AttachmentStatus.Available, snapshots.snapshot(id)?.status)
        Files.delete(temporaryFolder.root.toPath().resolve(original.filePath!!))
        val missing = assertNotNull(snapshots.snapshot(id))
        assertEquals(AttachmentStatus.Missing, missing.status)
        assertEquals(original, missing.record)
        assertNotNull(database.vacancyDao().findById(id))
    }
}
