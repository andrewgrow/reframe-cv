package reframecv.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import reframecv.assessment.ResumeAssessmentOutcome
import reframecv.assessment.ResumeAssessmentResult
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.ComponentTest

class AssessResumeUseCaseTest : ComponentTest() {
    private val resume = Resume(1, 7, "CV", 0, 5, photoPath = "/private/photo.png")
    private val vacancy =
        Vacancy(
            id = 3,
            projectId = 7,
            name = "Stored",
            description = "Kotlin",
            createdAt = 0,
            updatedAt = 0,
        )

    private suspend fun provider() = dependencies.providerSettingsUseCases.save(
        ProviderConnection(
            type = ApiProviderType.OpenRouter,
            name = "Work",
            configuration = OpenRouterConfiguration(
                apiKey = "secret",
                modelId = "model",
            ),
        ),
    )

    @Test
    fun existingVacancyPersistsOnlyTheAssessedLinkWithoutSavingOtherDraftFields() = runBlocking {
        val id = provider()
        dependencies.vacanciesRepository.records.value = listOf(vacancy)
        val draft = vacancy.copy(name = "Unsaved", resumes = listOf(VacancyResume(1)))
        dependencies.assessmentResponse =
            {
                ResumeAssessmentOutcome.Assessed(
                    ResumeAssessmentResult(85, "Strong Kotlin evidence."),
                )
            }
        val result = dependencies.assessResume(resume, draft, id, "Assess")
        val stored = dependencies.vacanciesRepository.records.value.single()
        assertEquals("Stored", stored.name)
        assertEquals(vacancy.description, stored.description)
        assertEquals(result, stored.resumes.single().assessment)
        assertEquals(85, stored.resumes.single().atsScore)
        assertFalse(dependencies.assessmentInputs.single().first.contains("photo.png"))
        assertFalse(dependencies.assessmentInputs.single().first.contains("photoPath"))
        assertEquals(result.vacancyJson, dependencies.assessmentInputs.single().second)
        assertFalse(result.vacancyJson.contains("photo.png"))
        assertTrue(result.vacancyJson.contains("Unsaved"))
    }

    @Test
    fun newVacancyReturnsDraftAssessmentAndFailuresOrCancellationNeverPersistIt() = runBlocking {
        val id = provider()
        val draft = vacancy.copy(id = 0, resumes = listOf(VacancyResume(1)))
        dependencies.assessmentResponse =
            { ResumeAssessmentOutcome.Assessed(ResumeAssessmentResult(0, "Not enough evidence.")) }
        assertEquals(0, dependencies.assessResume(resume, draft, id, "Assess").result.score)
        assertTrue(dependencies.vacanciesRepository.records.value.isEmpty())
        dependencies.assessmentResponse = { ResumeAssessmentOutcome.Failed("API key expired.") }
        val error =
            assertFailsWith<ResumeAssessmentException> {
                dependencies.assessResume(resume, draft, id, "Assess")
            }
        assertEquals("API key expired.", error.message)
        dependencies.assessmentResponse = { throw CancellationException("Closed") }
        assertFailsWith<CancellationException> {
            dependencies.assessResume(resume, draft, id, "Assess")
        }
        assertTrue(dependencies.vacanciesRepository.records.value.isEmpty())
    }

    @Test
    fun persistenceFailureIsReportedInsteadOfReturningAnUnsavedAssessment() = runBlocking<Unit> {
        val id = provider()
        dependencies.assessmentResponse =
            { ResumeAssessmentOutcome.Assessed(ResumeAssessmentResult(80, "Good match.")) }
        val draft = vacancy.copy(resumes = listOf(VacancyResume(1)))
        val error = assertFailsWith<ResumeAssessmentException> {
            dependencies.assessResume(resume, draft, id, "Assess")
        }
        assertEquals("The assessment could not be saved. Please retry.", error.message)
        assertTrue(dependencies.vacanciesRepository.records.value.isEmpty())
    }

    @Test
    fun disabledProviderAndCrossProjectResumesCannotBeAssessed() = runBlocking {
        val id = provider()
        dependencies.providerSettingsUseCases.setActive(id, false)
        var requests = 0
        dependencies.assessmentResponse = {
            requests++
            error("Must not execute")
        }
        val draft = vacancy.copy(resumes = listOf(VacancyResume(1)))
        assertFailsWith<Exception> { dependencies.assessResume(resume, draft, id, "Assess") }
        assertFailsWith<IllegalArgumentException> {
            dependencies.assessResume(resume.copy(projectId = 8), draft, id, "Assess")
        }
        assertEquals(0, requests)
    }
}
