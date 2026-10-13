package reframecv.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.project.ProjectMode
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft

class LocalResumesRepositoryTest : WorkspaceDatabaseTest() {
    @Test
    fun editedDraftKeepsSourceAndCreationDateAfterReopeningDatabase() = runBlocking<Unit> {
        val projectId = project()
        val repository = LocalResumesRepository(database.resumeDao())
        val sourceId = repository.create(resume(projectId))
        val derivedId = repository.create(
            resume(projectId).copy(
                sourceResumeId = sourceId,
                content = sampleResumeContent(),
            ),
        )
        val original = repository.observeResumes(projectId).first().single { it.id == derivedId }
        val draft = ResumeDraft.fromResume(original).copy(name = "Tailored", keywords = "Compose")
        repository.update(draft.applyTo(original))
        database.close()
        openDatabase()
        val records = LocalResumesRepository(database.resumeDao()).observeResumes(projectId).first()
        assertEquals(2, records.size)
        val saved = records.single { it.id == derivedId }
        assertEquals("Tailored", saved.name)
        assertEquals(listOf("Compose"), saved.keywords)
        assertEquals(sourceId, saved.sourceResumeId)
        assertEquals(original.createdAt, saved.createdAt)
        assertTrue(saved.updatedAt >= original.updatedAt)
        assertEquals(original.content, saved.content)
        assertEquals("Resume", records.single { it.id == sourceId }.name)
    }

    @Test
    fun creationDraftPersistsStructuredContentAndActivatesWorkspace() = runBlocking<Unit> {
        val projectId = project()
        val content = sampleResumeContent()
        val initial = ResumeDraft("  Mobile  ", profile = content.profile)
        val draft = content.sections.fold(initial) { value, section ->
            value.addSection(section)
        }
        val repository = LocalResumesRepository(database.resumeDao())
        val id = repository.create(draft.toResume(projectId))
        database.close()
        openDatabase()
        val saved = LocalResumesRepository(
            database.resumeDao(),
        ).observeResumes(projectId).first().single()
        assertEquals(id, saved.id)
        assertEquals("Mobile", saved.name)
        assertEquals(content, saved.content)
        assertTrue(saved.createdAt > 0)
        assertEquals(saved.createdAt, saved.updatedAt)
        assertEquals(ProjectMode.Workspace, database.projectDao().findById(projectId)?.mode)
    }

    @Test
    fun writesReadsSearchesUpdatesAndPermanentlyDeletes() = runBlocking<Unit> {
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
        assertEquals(null, database.resumeDao().findById(id))
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(projectId)?.mode)
        assertFailsWith<IllegalStateException> { repository.delete(id) }
        assertFailsWith<IllegalStateException> { repository.delete(-1) }
        assertFailsWith<IllegalStateException> { repository.update(updated) }
    }

    @Test
    fun scopesRecordsToOwnerAndRemovesDeletedProjectRecords() = runBlocking<Unit> {
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
        assertEquals(null, database.resumeDao().findById(id))
        assertFailsWith<IllegalStateException> { repository.create(resume(projectId)) }
    }
}
