package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.assessment.ResumeAssessment
import reframecv.assessment.ResumeAssessmentResult
import reframecv.domain.models.vacancy.VacancyResume

class VacancyResumesTest : WorkspaceDatabaseTest() {
    @Test
    fun assessmentUpsertsOneLinkAndSurvivesReopeningWithoutChangingVacancyFields() = runBlocking {
        val project = project()
        val firstResume = database.resumeDao().create(resume(project))
        val secondResume = database.resumeDao().create(resume(project))
        val id = database.vacancyDao().create(
            vacancy(project).copy(resumes = listOf(VacancyResume(firstResume, 20))),
        )
        val before = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        val assessment =
            ResumeAssessment(
                ResumeAssessmentResult(82, "Relevant experience."),
                300,
                100,
                "{}",
                "Work",
                "model",
            )
        database.vacancyDao().saveResumeAssessment(id, project, secondResume, assessment, 400)
        closeDatabase()
        openDatabase()
        val stored = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        assertEquals(
            before.copy(
                updatedAt = 400,
                resumes = listOf(
                    VacancyResume(firstResume, 20),
                    VacancyResume(secondResume, 82, assessment),
                ),
            ),
            stored,
        )
        val revised = assessment.copy(result = ResumeAssessmentResult(90, "Updated assessment."))
        database.vacancyDao().saveResumeAssessment(id, project, secondResume, revised, 500)
        val links = assertNotNull(database.vacancyDao().findById(id)).toDomainModel().resumes
        assertEquals(listOf(firstResume, secondResume), links.map { it.resumeId })
        assertEquals(revised, links.last().assessment)
        val foreign = database.resumeDao().create(resume(project("Other")))
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().saveResumeAssessment(id, project, foreign, revised, 600)
        }
        assertEquals(links, database.vacancyDao().findById(id)?.toDomainModel()?.resumes)
        database.resumeDao().delete(secondResume, 700)
        assertEquals(
            listOf(VacancyResume(firstResume, 20)),
            database.vacancyDao().findById(id)?.toDomainModel()?.resumes,
        )
    }

    @Test
    fun persistsOrderedLinksAndIndependentScoresAcrossReopeningAndUnlinking() = runBlocking {
        val project = project()
        val firstResume = database.resumeDao().create(resume(project))
        val secondResume = database.resumeDao().create(resume(project).copy(name = "Second"))
        val first = database.vacancyDao().create(
            vacancy(project).copy(
                resumes = listOf(VacancyResume(secondResume, 78), VacancyResume(firstResume)),
            ),
        )
        val second = database.vacancyDao().create(
            vacancy(project).copy(
                resumes = listOf(VacancyResume(secondResume, 35)),
            ),
        )
        closeDatabase()
        openDatabase()
        val stored = assertNotNull(database.vacancyDao().findById(first)).toDomainModel()
        assertEquals(
            listOf(VacancyResume(secondResume, 78), VacancyResume(firstResume)),
            stored.resumes,
        )
        assertEquals(
            listOf(VacancyResume(secondResume, 35)),
            database.vacancyDao().findById(second)?.toDomainModel()?.resumes,
        )
        database.vacancyDao().update(stored.copy(resumes = listOf(VacancyResume(firstResume))))
        assertEquals(
            listOf(VacancyResume(firstResume)),
            database.vacancyDao().findById(first)?.toDomainModel()?.resumes,
        )
        assertNotNull(database.resumeDao().findById(secondResume))
        assertEquals(
            listOf(VacancyResume(secondResume, 35)),
            database.vacancyDao().findById(second)?.toDomainModel()?.resumes,
        )
    }

    @Test
    fun deletingResumeOrVacancyCascadesOnlyTheirLinks() = runBlocking {
        val project = project()
        val firstResume = database.resumeDao().create(resume(project))
        val secondResume = database.resumeDao().create(resume(project))
        val links = listOf(VacancyResume(firstResume, 100), VacancyResume(secondResume, 60))
        val first = database.vacancyDao().create(vacancy(project).copy(resumes = links))
        val second = database.vacancyDao().create(vacancy(project).copy(resumes = links))
        database.resumeDao().delete(firstResume, 300)
        val remaining = database.vacancyDao().observeActive(project).first()
        assertEquals(2, remaining.size)
        remaining.forEach {
            assertEquals(listOf(VacancyResume(secondResume, 60)), it.toDomainModel().resumes)
        }
        database.vacancyDao().delete(first, 400)
        assertNull(database.vacancyDao().findById(first))
        assertNotNull(database.resumeDao().findById(secondResume))
        assertEquals(
            listOf(VacancyResume(secondResume, 60)),
            database.vacancyDao().findById(second)?.toDomainModel()?.resumes,
        )
        database.projectDao().deleteSubtree(project)
        assertNull(database.vacancyDao().findById(second))
        assertNull(database.resumeDao().findById(secondResume))
    }

    @Test
    fun rejectsDuplicateForeignAndMissingLinksWithoutPartialUpdates() = runBlocking {
        val project = project()
        val other = project("Other")
        val resume = database.resumeDao().create(resume(project))
        val foreign = database.resumeDao().create(resume(other))
        val id = database.vacancyDao().create(
            vacancy(project).copy(resumes = listOf(VacancyResume(resume, 45))),
        )
        val stored = assertNotNull(database.vacancyDao().findById(id)).toDomainModel()
        for (links in listOf(
            listOf(VacancyResume(resume), VacancyResume(resume)),
            listOf(VacancyResume(foreign)),
            listOf(VacancyResume(-1)),
        )) {
            assertFailsWith<IllegalArgumentException> {
                database.vacancyDao().update(stored.copy(name = "Changed", resumes = links))
            }
            assertEquals(stored, database.vacancyDao().findById(id)?.toDomainModel())
        }
        for (score in listOf(-1, 101)) {
            assertFailsWith<IllegalArgumentException> { VacancyResume(resume, score) }
        }
    }
}
