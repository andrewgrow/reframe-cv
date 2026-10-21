package reframecv.database.workspace

import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import reframecv.database.coverletter.CoverLetterEntity
import reframecv.database.resume.ResumeEntity
import reframecv.database.resume.ResumeKeywordEntity
import reframecv.database.vacancy.VacancyEntity
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.vacancy.VacancyResume

class WorkspaceRelationsTest : WorkspaceDatabaseTest() {
    @Test
    fun foreignKeysRejectMissingRecordsAndKeywordsWithoutOwners() = runBlocking<Unit> {
        val projectId = project()
        assertFailsWith<SQLiteException> {
            database.resumeDao().insertEntity(ResumeEntity.fromDomainModel(resume(-1)))
        }
        assertFailsWith<SQLiteException> {
            database.coverLetterDao().insertEntity(CoverLetterEntity.fromDomainModel(letter(-1)))
        }
        assertFailsWith<SQLiteException> {
            database.vacancyDao().insertEntity(VacancyEntity.fromDomainModel(vacancy(-1)))
        }
        assertFailsWith<SQLiteException> {
            database.resumeDao().insertEntity(
                ResumeEntity.fromDomainModel(resume(projectId).copy(sourceResumeId = -1)),
            )
        }
        assertFailsWith<SQLiteException> {
            database.vacancyDao().insertResumes(
                listOf(reframecv.database.vacancy.VacancyResumeEntity(-1, -1)),
            )
        }
        assertFailsWith<SQLiteException> {
            database.vacancyDao().insertLetters(
                listOf(reframecv.database.vacancy.VacancyLetterEntity(-1, -1, 0)),
            )
        }
        assertFailsWith<SQLiteException> {
            database.resumeDao().insertKeywords(
                listOf(ResumeKeywordEntity(-1, "Kotlin", "kotlin", 0)),
            )
        }
        assertFailsWith<SQLiteException> {
            database.projectDao().update(
                requireNotNull(database.projectDao().findById(projectId)).copy(mainResumeId = -1),
            )
        }
    }

    @Test
    fun mainResumeRequiresAnActiveResumeFromTheSameProject() = runBlocking {
        val projectId = project()
        val otherId = project("Other")
        val resumeId = database.resumeDao().create(resume(projectId))
        val otherResume = database.resumeDao().create(resume(otherId))
        val dao = database.projectDao()
        assertEquals(0, dao.setMainResume(projectId, -1, 300))
        assertEquals(0, dao.setMainResume(projectId, otherResume, 300))
        assertEquals(1, dao.setMainResume(projectId, resumeId, 300))
        assertEquals(resumeId, dao.findById(projectId)?.mainResumeId)
        assertEquals(1, dao.setMainResume(projectId, null, 400))
        assertNull(dao.findById(projectId)?.mainResumeId)
        database.resumeDao().delete(resumeId, 500)
        assertEquals(0, dao.setMainResume(projectId, resumeId, 600))
        assertEquals(0, dao.setMainResume(-1, otherResume, 600))
        dao.deleteSubtree(otherId)
        assertEquals(0, dao.setMainResume(otherId, otherResume, 800))
    }

    @Test
    fun deletionClearsMainSourceAndAllVacancyLinksButPreservesTexts() = runBlocking {
        val projectId = project()
        val resumeId = database.resumeDao().create(resume(projectId))
        val adaptedId = database.resumeDao().create(
            resume(
                projectId,
            ).copy(
                sourceResumeId = resumeId,
                content = ResumeContent(sections = listOf(ResumeSummarySection(text = "Adapted"))),
            ),
        )
        val letterId = database.coverLetterDao().create(letter(projectId))
        val first = database.vacancyDao().create(
            vacancy(
                projectId,
            ).copy(resumes = listOf(VacancyResume(resumeId)), coverLetterIds = listOf(letterId)),
        )
        val second = database.vacancyDao().create(
            vacancy(
                projectId,
            ).copy(resumes = listOf(VacancyResume(resumeId)), coverLetterIds = listOf(letterId)),
        )
        database.projectDao().setMainResume(projectId, resumeId, 300)
        assertEquals(1, database.resumeDao().delete(resumeId, 400))
        assertNull(database.projectDao().findById(projectId)?.mainResumeId)
        assertNull(database.resumeDao().findById(adaptedId)?.entity?.sourceResumeId)
        assertEquals(
            ResumeContent(sections = listOf(ResumeSummarySection(text = "Adapted"))),
            database.resumeDao().findById(adaptedId)?.toDomainModel()?.content,
        )
        for (id in listOf(first, second)) {
            assertEquals(emptyList(), database.vacancyDao().findById(id)?.toDomainModel()?.resumes)
            assertEquals(
                listOf(letterId),
                database.vacancyDao().findById(id)?.toDomainModel()?.coverLetterIds,
            )
        }
        assertEquals(1, database.coverLetterDao().delete(letterId, 500))
        for (id in listOf(first, second)) {
            assertEquals(
                emptyList(),
                database.vacancyDao().findById(id)?.toDomainModel()?.coverLetterIds,
            )
            assertEquals("Job description", database.vacancyDao().findById(id)?.entity?.description)
        }
        assertEquals(0, database.resumeDao().delete(resumeId, 600))
        assertEquals(null, database.resumeDao().findById(resumeId))
        assertEquals(0, database.coverLetterDao().delete(letterId, 600))
        assertEquals(0, database.vacancyDao().delete(-1, 600))
    }

    @Test
    fun rejectsCrossProjectDeletedAndMissingReferencesWithoutChangingProject() = runBlocking {
        val projectId = project()
        val otherId = project("Other")
        val resumeId = database.resumeDao().create(resume(otherId))
        val letterId = database.coverLetterDao().create(letter(otherId))
        val original = database.projectDao().findById(projectId)
        assertFailsWith<IllegalArgumentException> {
            database.resumeDao().create(resume(projectId).copy(sourceResumeId = resumeId))
        }
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().create(
                vacancy(projectId).copy(resumes = listOf(VacancyResume(resumeId))),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().create(vacancy(projectId).copy(coverLetterIds = listOf(letterId)))
        }
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().create(
                vacancy(projectId).copy(resumes = listOf(VacancyResume(-1))),
            )
        }
        database.resumeDao().delete(resumeId, 300)
        database.coverLetterDao().delete(letterId, 300)
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().create(
                vacancy(otherId).copy(resumes = listOf(VacancyResume(resumeId))),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().create(vacancy(otherId).copy(coverLetterIds = listOf(letterId)))
        }
        assertEquals(original, database.projectDao().findById(projectId))
    }
}
