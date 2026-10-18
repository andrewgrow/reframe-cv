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
        database.vacancyAttachmentDao(),
        FileAttachmentStorage(temporaryFolder.root.toPath()),
        ApplicationLogger.None,
    )

    private fun attachment(name: String = "source.txt"): SnapshotChange.Attach {
        val file = temporaryFolder.newFile(name)
        file.writeText("Original source: $name")
        return SnapshotChange.Attach(SelectedAttachment(file.path, name))
    }

    @Test
    fun persistsOriginalSnapshotAndKeepsItWhenVacancyChanges() = runBlocking<Unit> {
        val snapshots = manager()
        val id = snapshots.create(vacancy(project()), attachment())
        val original = assertNotNull(snapshots.snapshot(id))
        assertEquals("source.txt", original.record.fileName)
        val vacancy = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        snapshots.update(vacancy.copy(name = "Edited", url = "changed"), SnapshotChange.Keep)
        assertEquals(original, manager().snapshot(id))
        assertEquals(AttachmentStatus.Available, original.status)
    }

    @Test
    fun persistsParsingWithNewAndExistingSourcesAndResetsItOnReplacement() = runBlocking<Unit> {
        val snapshots = manager()
        val id = snapshots.create(vacancy(project()), attachment().copy(parsedAt = 500))
        val source = assertNotNull(snapshots.snapshot(id)).record
        assertEquals(500L, source.parsedAt)
        val record = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        snapshots.update(record.copy(name = "Edited"), SnapshotChange.Keep)
        assertEquals(500L, assertNotNull(snapshots.snapshot(id)).record.parsedAt)
        snapshots.update(record, SnapshotChange.Parsed(source.id, 600))
        database.close()
        openDatabase()
        val reopened = assertNotNull(manager().snapshot(id)).record
        assertEquals(source.copy(parsedAt = 600), reopened)
        manager().update(record, attachment("new.pdf"))
        assertNull(assertNotNull(manager().snapshot(id)).record.parsedAt)
        manager().update(record, SnapshotChange.Remove)
        assertNull(manager().snapshot(id))
    }

    @Test
    fun failedSaveAndMismatchedSourceCannotPersistParsingStatus() = runBlocking<Unit> {
        val snapshots = manager()
        val projectId = project()
        val id = snapshots.create(vacancy(projectId), attachment().copy(parsedAt = 500))
        val source = assertNotNull(snapshots.snapshot(id)).record
        val record = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        assertFailsWith<IllegalArgumentException> {
            snapshots.update(
                record.copy(projectId = projectId + 1),
                SnapshotChange.Parsed(source.id, 600),
            )
        }
        assertEquals(500L, assertNotNull(snapshots.snapshot(id)).record.parsedAt)
        assertEquals(record, database.vacancyDao().findById(id)?.toDomainModel())
        assertFailsWith<IllegalStateException> {
            snapshots.update(record, SnapshotChange.Parsed(source.id + 1, 700))
        }
        assertEquals(500L, assertNotNull(snapshots.snapshot(id)).record.parsedAt)
        assertFailsWith<IllegalArgumentException> {
            snapshots.create(vacancy(projectId), SnapshotChange.Parsed(source.id, 800))
        }
    }

    @Test
    fun rollbackRemovesCopiedFileAndSnapshotRecord() = runBlocking<Unit> {
        assertFailsWith<IllegalStateException> {
            manager().create(vacancy(999), attachment())
        }
        assertTrue(database.vacancyAttachmentDao().filePaths().isEmpty())
        Files.list(temporaryFolder.root.toPath().resolve("attachments")).use {
            assertEquals(0L, it.count())
        }
    }

    @Test
    fun replacementAndDetachRemoveOnlyUnusedCopies() = runBlocking<Unit> {
        val snapshots = manager()
        val projectId = project()
        val id = snapshots.create(vacancy(projectId), attachment())
        val original = assertNotNull(snapshots.snapshot(id)).record
        val shared = database.vacancyDao().create(
            vacancy(projectId).copy(vacancyAttachmentId = original.id),
        )
        val record = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        snapshots.update(record, attachment("replacement.pdf"))
        val replacement = assertNotNull(snapshots.snapshot(id)).record
        assertTrue(Files.exists(temporaryFolder.root.toPath().resolve(original.filePath)))
        snapshots.delete(shared, 300)
        assertTrue(!Files.exists(temporaryFolder.root.toPath().resolve(original.filePath)))
        snapshots.update(record, SnapshotChange.Remove)
        assertNull(snapshots.snapshot(id))
        assertTrue(!Files.exists(temporaryFolder.root.toPath().resolve(replacement.filePath)))
        assertTrue(database.vacancyAttachmentDao().filePaths().isEmpty())
    }

    @Test
    fun missingFilePreservesMetadataUntilProjectDeletion() = runBlocking<Unit> {
        val snapshots = manager()
        val projectId = project()
        val id = snapshots.create(vacancy(projectId), attachment())
        val original = assertNotNull(snapshots.snapshot(id)).record
        Files.delete(temporaryFolder.root.toPath().resolve(original.filePath))
        val missing = assertNotNull(snapshots.snapshot(id))
        assertEquals(AttachmentStatus.Missing, missing.status)
        assertEquals(original, missing.record)
        assertNotNull(database.vacancyDao().findById(id))
        assertEquals(1, snapshots.deleteProject(database.projectDao(), projectId))
        assertNull(database.vacancyDao().findById(id))
        assertNull(database.vacancyAttachmentDao().findById(original.id))
    }
}
