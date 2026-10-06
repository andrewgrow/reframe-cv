package reframecv.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.project.ProjectMode

class LocalVacanciesRepositoryTest : WorkspaceDatabaseTest() {
    @Test
    fun writesReadsSearchesUpdatesAndSoftDeletes() = runBlocking<Unit> {
        val projectId = project()
        val repository = LocalVacanciesRepository(database.vacancyDao())
        val draft = vacancy(projectId)
        val id = repository.create(draft)
        val saved = repository.observeVacancies(projectId).first().single()
        assertEquals(id, saved.id)
        assertTrue(saved.createdAt > draft.createdAt)
        assertEquals(saved.createdAt, saved.updatedAt)
        assertEquals(listOf("Kotlin", "Android", "Résumé"), saved.keywords)
        assertEquals(listOf(saved), repository.search(projectId, " KOTLIN "))
        assertEquals(ProjectMode.Workspace, database.projectDao().findById(projectId)?.mode)
        assertFailsWith<IllegalArgumentException> { repository.create(saved) }

        repository.update(saved.copy(name = "Updated", keywords = listOf(" JVM ")))
        val updated = repository.observeVacancies(projectId).first().single()
        assertEquals("Updated", updated.name)
        assertEquals(saved.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt >= saved.updatedAt)
        assertEquals(listOf("JVM"), updated.keywords)
        assertEquals(emptyList(), repository.search(projectId, "Kotlin"))
        repository.delete(id)
        assertEquals(emptyList(), repository.observeVacancies(projectId).first())
        assertEquals(emptyList(), repository.search(projectId, "JVM"))
        assertTrue(database.vacancyDao().findById(id)?.entity?.deletedAt != null)
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(projectId)?.mode)
        assertFailsWith<IllegalStateException> { repository.delete(id) }
        assertFailsWith<IllegalStateException> { repository.delete(-1) }
        assertFailsWith<IllegalStateException> { repository.update(updated) }
    }

    @Test
    fun scopesRecordsToActiveOwnerAndHidesDeletedProjects() = runBlocking<Unit> {
        val projectId = project()
        val otherId = project("Other")
        val repository = LocalVacanciesRepository(database.vacancyDao())
        val id = repository.create(vacancy(projectId))
        val otherRecordId = repository.create(vacancy(otherId))
        assertEquals(listOf(id), repository.observeVacancies(projectId).first().map { it.id })
        LocalProjectsRepository(database.projectDao()).deleteProject(projectId)
        assertEquals(emptyList(), repository.observeVacancies(projectId).first())
        assertEquals(emptyList(), repository.search(projectId, "Kotlin"))
        assertEquals(
            listOf(otherRecordId),
            repository.observeVacancies(otherId).first().map {
                it.id
            },
        )
        assertEquals(null, database.vacancyDao().findById(id)?.entity?.deletedAt)
        assertFailsWith<IllegalStateException> { repository.create(vacancy(projectId)) }
    }
}
