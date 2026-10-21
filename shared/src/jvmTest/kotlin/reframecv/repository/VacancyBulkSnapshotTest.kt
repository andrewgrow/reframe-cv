package reframecv.repository

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.attachments.TestAttachmentStorage
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.logging.ApplicationLogger

class VacancyBulkSnapshotTest : WorkspaceDatabaseTest() {
    @Test
    fun bulkDeleteCleansOnlyUnreferencedFilesAfterCommit() = runBlocking<Unit> {
        val files = TestAttachmentStorage()
        val snapshots =
            VacancySnapshotManager(
                database.vacancyDao(),
                database.vacancyAttachmentDao(),
                files,
                ApplicationLogger.None,
            )
        val repository = LocalVacanciesRepository(database.vacancyDao(), snapshots)
        val project = project()
        val attach = SnapshotChange.Attach(SelectedAttachment("source", "job.txt"))
        val first = repository.createSaved(vacancy(project), attach)
        val second = repository.createSaved(vacancy(project), attach)
        val shared = repository.create(
            vacancy(project).copy(vacancyAttachmentId = first.vacancyAttachmentId),
        )
        assertEquals(2, files.files.size)
        repository.deleteMany(project, setOf(first.id, second.id))
        assertEquals(1, files.files.size)
        assertEquals(shared, repository.observeVacancies(project).first().single().id)
        repository.deleteMany(project, setOf(shared))
        assertEquals(0, files.files.size)
        assertEquals(emptyList(), database.vacancyAttachmentDao().filePaths())
    }

    @Test
    fun failedBulkTransactionKeepsEverySourceFile() = runBlocking<Unit> {
        val files = TestAttachmentStorage()
        val snapshots =
            VacancySnapshotManager(
                database.vacancyDao(),
                database.vacancyAttachmentDao(),
                files,
                ApplicationLogger.None,
            )
        val repository = LocalVacanciesRepository(database.vacancyDao(), snapshots)
        val project = project()
        val attach = SnapshotChange.Attach(SelectedAttachment("source", "job.txt"))
        val first = repository.create(vacancy(project), attach)
        val second = repository.create(vacancy(project), attach)
        val paths = files.files.keys.toSet()
        database.useWriterConnection { connection ->
            connection.executeSQL(
                "CREATE TRIGGER reject_bulk BEFORE DELETE ON vacancies " +
                    "WHEN OLD.id = $second BEGIN SELECT RAISE(ABORT, 'Delete failure'); END",
            )
        }
        assertFailsWith<SQLiteException> {
            repository.deleteMany(project, linkedSetOf(first, second))
        }
        assertEquals(paths, files.files.keys)
        assertEquals(2, repository.observeVacancies(project).first().size)
    }
}
