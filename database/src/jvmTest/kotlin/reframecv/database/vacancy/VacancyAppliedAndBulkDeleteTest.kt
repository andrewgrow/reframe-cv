package reframecv.database.vacancy

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.project.ProjectMode
import reframecv.repository.LocalVacanciesRepository

class VacancyAppliedAndBulkDeleteTest : WorkspaceDatabaseTest() {
    @Test
    fun appliedTimestampCanBeCreatedUpdatedAndCleared() = runBlocking<Unit> {
        val repository = LocalVacanciesRepository(database.vacancyDao())
        val project = project()
        val id = repository.create(vacancy(project).copy(appliedAt = 123_000))
        val saved = repository.observeVacancies(project).first().single()
        assertEquals(123_000L, saved.appliedAt)
        repository.update(saved.copy(appliedAt = 456_000))
        assertEquals(456_000L, database.vacancyDao().findById(id)?.toDomainModel()?.appliedAt)
        repository.update(saved.copy(appliedAt = null))
        assertNull(repository.observeVacancies(project).first().single().appliedAt)
    }

    @Test
    fun bulkDeleteIsProjectScopedAndPreservesUnselectedDocuments() = runBlocking<Unit> {
        val repository = LocalVacanciesRepository(database.vacancyDao())
        val project = project()
        val ids = (1..3).map { repository.create(vacancy(project).copy(name = "Job $it")) }
        val otherProject = project("Other")
        val otherId = repository.create(vacancy(otherProject))
        assertFailsWith<IllegalArgumentException> {
            repository.deleteMany(project, setOf(ids.first(), otherId))
        }
        assertEquals(3, repository.observeVacancies(project).first().size)
        repository.deleteMany(project, ids.take(2).toSet())
        assertEquals(listOf(ids.last()), repository.observeVacancies(project).first().map { it.id })
        assertEquals(otherId, repository.observeVacancies(otherProject).first().single().id)
        repository.deleteMany(project, setOf(ids.last()))
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(project)?.mode)
    }

    @Test
    fun failureDuringBulkDeleteRollsBackEarlierDeletions() = runBlocking<Unit> {
        val repository = LocalVacanciesRepository(database.vacancyDao())
        val project = project()
        val first = repository.create(vacancy(project))
        val second = repository.create(vacancy(project))
        database.useWriterConnection { connection ->
            connection.executeSQL(
                "CREATE TRIGGER reject_vacancy BEFORE DELETE ON vacancies " +
                    "WHEN OLD.id = $second BEGIN SELECT RAISE(ABORT, 'Delete failure'); END",
            )
        }
        assertFailsWith<SQLiteException> {
            repository.deleteMany(project, linkedSetOf(first, second))
        }
        assertEquals(
            setOf(first, second),
            repository.observeVacancies(project).first().map {
                it.id
            }.toSet(),
        )
        assertTrue(database.projectDao().findById(project)?.mode == ProjectMode.Workspace)
    }
}
