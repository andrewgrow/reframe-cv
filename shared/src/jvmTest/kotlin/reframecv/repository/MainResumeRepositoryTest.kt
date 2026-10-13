package reframecv.repository

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.project.ProjectMode

class MainResumeRepositoryTest : WorkspaceDatabaseTest() {
    @Test
    fun deletingLastMainResumeClearsChoiceAndResetsWorkspace() = runBlocking<Unit> {
        val projectId = project()
        val repository = LocalResumesRepository(database.resumeDao())
        val id = repository.create(resume(projectId), MainResumeChange.Select)
        repository.delete(id)
        assertNull(repository.observeMainResumeId(projectId).first())
        assertEquals(emptyList(), repository.observeResumes(projectId).first())
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(projectId)?.mode)
    }

    @Test
    fun createsReplacesAndClearsMainResumeWithoutClearingAnotherSelection() = runBlocking {
        val projectId = project()
        val repository = LocalResumesRepository(database.resumeDao())
        val first = repository.create(resume(projectId), MainResumeChange.Select)
        assertEquals(first, repository.observeMainResumeId(projectId).first())
        val second = repository.create(resume(projectId))
        var secondRecord = repository.observeResumes(projectId).first().single { it.id == second }
        repository.update(secondRecord.copy(name = "Main"), MainResumeChange.Select)
        assertEquals(second, repository.observeMainResumeId(projectId).first())
        val firstRecord = repository.observeResumes(projectId).first().single { it.id == first }
        repository.update(firstRecord, MainResumeChange.Clear)
        assertEquals(second, repository.observeMainResumeId(projectId).first())
        repository.update(firstRecord.copy(name = "Other"))
        assertEquals(second, repository.observeMainResumeId(projectId).first())
        database.close()
        openDatabase()
        val reopened = LocalResumesRepository(database.resumeDao())
        assertEquals(second, reopened.observeMainResumeId(projectId).first())
        secondRecord = reopened.observeResumes(projectId).first().single { it.id == second }
        reopened.update(secondRecord, MainResumeChange.Clear)
        assertNull(reopened.observeMainResumeId(projectId).first())
        assertEquals(2, reopened.observeResumes(projectId).first().size)
        assertEquals(0, database.resumeDao().selectMainResume(project("Other"), second, 100))
    }

    @Test
    fun failedMainSelectionRollsBackCreationAndContentUpdates() = runBlocking {
        val projectId = project()
        val repository = LocalResumesRepository(database.resumeDao())
        repository.create(resume(projectId), MainResumeChange.Select)
        val original = repository.observeResumes(projectId).first().single()
        val originalProject = database.projectDao().findById(projectId)
        database.useWriterConnection { connection ->
            connection.executeSQL(
                "CREATE TRIGGER reject_main BEFORE UPDATE OF main_resume_id ON projects " +
                    "BEGIN SELECT RAISE(ABORT, 'Main selection failure'); END",
            )
        }
        assertFailsWith<SQLiteException> {
            repository.create(resume(projectId).copy(name = "New"), MainResumeChange.Select)
        }
        assertFailsWith<SQLiteException> {
            repository.update(
                original.copy(name = "Changed", keywords = listOf("Java")),
                MainResumeChange.Select,
            )
        }
        assertEquals(listOf(original), repository.observeResumes(projectId).first())
        assertEquals(originalProject, database.projectDao().findById(projectId))
        assertEquals(original.id, repository.observeMainResumeId(projectId).first())
    }
}
