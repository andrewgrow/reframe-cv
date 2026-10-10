package reframecv.database.importrecord

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.vacancy.VacancyEntity
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.project.ProjectMode

class VacancyDeletionTest : WorkspaceDatabaseTest() {
    private suspend fun source(): Long = database.importRecordDao().insert(
        ImportRecordEntity(provider = "test", rawData = "{}", createdAt = 100),
    )

    @Test
    fun removesVacancyKeywordsAndUnusedSourceButPreservesResumeAndLetter() = runBlocking {
        val projectId = project()
        val sourceId = source()
        val resumeId = database.resumeDao().create(resume(projectId))
        val letterId = database.coverLetterDao().create(letter(projectId))
        val originalResume = database.resumeDao().findById(resumeId)?.toDomainModel()
        val originalLetter = database.coverLetterDao().findById(letterId)?.toDomainModel()
        val record = vacancy(projectId).copy(
            importRecordId = sourceId,
            resumeId = resumeId,
            coverLetterId = letterId,
        )
        val dao = database.vacancyDao()
        val id = dao.create(record)
        assertEquals(1, dao.delete(id, 500))
        assertNull(dao.findById(id))
        assertNull(database.importRecordDao().findById(sourceId))
        assertEquals(emptyList(), dao.observeActive(projectId).first())
        assertEquals(0, dao.observeCount(projectId).first())
        assertEquals(originalResume, database.resumeDao().findById(resumeId)?.toDomainModel())
        assertEquals(originalLetter, database.coverLetterDao().findById(letterId)?.toDomainModel())
        assertEquals(ProjectMode.Workspace, database.projectDao().findById(projectId)?.mode)
        dao.insertEntity(VacancyEntity.fromDomainModel(record.copy(id = id, importRecordId = null)))
        assertEquals(emptyList(), dao.findById(id)?.keywords)
    }

    @Test
    fun retainsSharedSourceEvenWhenRemainingVacancyBelongsToDeletedProject() = runBlocking {
        val firstProject = project()
        val secondProject = project("Other")
        val sourceId = source()
        val dao = database.vacancyDao()
        val first = dao.create(vacancy(firstProject).copy(importRecordId = sourceId))
        val second = dao.create(vacancy(secondProject).copy(importRecordId = sourceId))
        val before = dao.findById(second)?.toDomainModel()
        database.projectDao().markSubtreeDeleted(secondProject, 400)
        assertEquals(1, dao.delete(first, 500))
        assertEquals(sourceId, database.importRecordDao().findById(sourceId)?.id)
        assertEquals(before, dao.findById(second)?.toDomainModel())
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(firstProject)?.mode)
        assertEquals(1, dao.delete(second, 600))
        assertNull(database.importRecordDao().findById(sourceId))
        assertEquals(0, dao.delete(second, 700))
        assertEquals(0, dao.delete(-1, 700))
    }

    @Test
    fun deletingLastVacancyRemovesSourceAndResetsWorkspace() = runBlocking {
        val sourceId = source()
        val projectId = project()
        val dao = database.vacancyDao()
        val id = dao.create(vacancy(projectId).copy(importRecordId = sourceId))
        assertEquals(1, dao.delete(id, 500))
        assertNull(database.importRecordDao().findById(sourceId))
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(projectId)?.mode)
        assertEquals(500L, database.projectDao().findById(projectId)?.updatedAt)
    }
}
