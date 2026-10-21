package reframecv.database.vacancyattachment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.vacancy.VacancyEntity
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.project.ProjectMode
import reframecv.domain.models.vacancy.VacancyResume

class VacancyDeletionTest : WorkspaceDatabaseTest() {
    private suspend fun source(): Long = database.vacancyAttachmentDao().insert(
        VacancyAttachmentEntity(
            createdAt = 100,
            filePath = "attachments/source.txt",
            fileName = "source.txt",
            mediaType = "text/plain",
            fileSize = 2,
            sha256 = "hash",
        ),
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
            vacancyAttachmentId = sourceId,
            resumes = listOf(VacancyResume(resumeId)),
            coverLetterIds = listOf(letterId),
        )
        val dao = database.vacancyDao()
        val id = dao.create(record)
        assertEquals(1, dao.delete(id, 500))
        assertNull(dao.findById(id))
        assertNull(database.vacancyAttachmentDao().findById(sourceId))
        assertEquals(emptyList(), dao.observeActive(projectId).first())
        assertEquals(0, dao.observeCount(projectId).first())
        assertEquals(originalResume, database.resumeDao().findById(resumeId)?.toDomainModel())
        assertEquals(originalLetter, database.coverLetterDao().findById(letterId)?.toDomainModel())
        assertEquals(ProjectMode.Workspace, database.projectDao().findById(projectId)?.mode)
        dao.insertEntity(
            VacancyEntity.fromDomainModel(record.copy(id = id, vacancyAttachmentId = null)),
        )
        assertEquals(emptyList(), dao.findById(id)?.keywords)
    }

    @Test
    fun projectDeletionPreservesSharedSourceUntilLastVacancyIsDeleted() = runBlocking {
        val firstProject = project()
        val secondProject = project("Other")
        val sourceId = source()
        val dao = database.vacancyDao()
        val first = dao.create(vacancy(firstProject).copy(vacancyAttachmentId = sourceId))
        val second = dao.create(vacancy(secondProject).copy(vacancyAttachmentId = sourceId))
        database.projectDao().deleteSubtree(secondProject)
        assertNull(dao.findById(second))
        assertEquals(sourceId, database.vacancyAttachmentDao().findById(sourceId)?.id)
        assertEquals(1, dao.delete(first, 500))
        assertNull(database.vacancyAttachmentDao().findById(sourceId))
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(firstProject)?.mode)
        assertEquals(0, dao.delete(second, 700))
        assertEquals(0, dao.delete(-1, 700))
    }

    @Test
    fun deletingLastVacancyRemovesSourceAndResetsWorkspace() = runBlocking {
        val sourceId = source()
        val projectId = project()
        val dao = database.vacancyDao()
        val id = dao.create(vacancy(projectId).copy(vacancyAttachmentId = sourceId))
        assertEquals(1, dao.delete(id, 500))
        assertNull(database.vacancyAttachmentDao().findById(sourceId))
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(projectId)?.mode)
        assertEquals(500L, database.projectDao().findById(projectId)?.updatedAt)
    }
}
