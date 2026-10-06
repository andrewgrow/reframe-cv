package reframecv.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.project.ProjectMode

class LocalResumesRepositoryTest : WorkspaceDatabaseTest() {
    @Test
    fun writesReadsSearchesUpdatesAndSoftDeletes() = runBlocking<Unit> {
        val projectId = project()
        val repository = LocalResumesRepository(database.resumeDao())
        val draft = resume(projectId)
        val id = repository.create(draft)
        val saved = repository.observeResumes(projectId).first().single()
        assertEquals(id, saved.id)
        assertTrue(saved.createdAt > draft.createdAt)
        assertEquals(saved.createdAt, saved.updatedAt)
        assertEquals(listOf("Kotlin", "Android", "Résumé"), saved.keywords)
        assertEquals(listOf(saved), repository.search(projectId, " KOTLIN "))
        assertEquals(ProjectMode.Workspace, database.projectDao().findById(projectId)?.mode)
        assertFailsWith<IllegalArgumentException> { repository.create(saved) }

        repository.update(saved.copy(name = "Updated", keywords = listOf(" JVM ")))
        val updated = repository.observeResumes(projectId).first().single()
        assertEquals("Updated", updated.name)
        assertEquals(saved.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt >= saved.updatedAt)
        assertEquals(listOf("JVM"), updated.keywords)
        assertEquals(emptyList(), repository.search(projectId, "Kotlin"))
        repository.delete(id)
        assertEquals(emptyList(), repository.observeResumes(projectId).first())
        assertEquals(emptyList(), repository.search(projectId, "JVM"))
        assertTrue(database.resumeDao().findById(id)?.entity?.deletedAt != null)
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(projectId)?.mode)
        assertFailsWith<IllegalStateException> { repository.delete(id) }
        assertFailsWith<IllegalStateException> { repository.delete(-1) }
        assertFailsWith<IllegalStateException> { repository.update(updated) }
    }

    @Test
    fun scopesRecordsToActiveOwnerAndHidesDeletedProjects() = runBlocking<Unit> {
        val projectId = project()
        val otherId = project("Other")
        val repository = LocalResumesRepository(database.resumeDao())
        val id = repository.create(resume(projectId))
        val otherRecordId = repository.create(resume(otherId))
        assertEquals(listOf(id), repository.observeResumes(projectId).first().map { it.id })
        LocalProjectsRepository(database.projectDao()).deleteProject(projectId)
        assertEquals(emptyList(), repository.observeResumes(projectId).first())
        assertEquals(emptyList(), repository.search(projectId, "Kotlin"))
        assertEquals(
            listOf(otherRecordId),
            repository.observeResumes(otherId).first().map {
                it.id
            },
        )
        assertEquals(null, database.resumeDao().findById(id)?.entity?.deletedAt)
        assertFailsWith<IllegalStateException> { repository.create(resume(projectId)) }
    }
}
