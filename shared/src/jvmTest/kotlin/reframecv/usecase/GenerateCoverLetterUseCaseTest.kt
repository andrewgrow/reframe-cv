package reframecv.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import reframecv.coverletters.CoverLetterGenerationOutcome
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.ComponentTest

class GenerateCoverLetterUseCaseTest : ComponentTest() {
    private val resume =
        Resume(
            id = 1,
            projectId = 7,
            name = "Main",
            createdAt = 0,
            updatedAt = 0,
            keywords = listOf("private"),
            photoPath = "secret.jpg",
        )
    private val vacancy =
        Vacancy(
            id = 2,
            projectId = 7,
            name = "ExampleCo",
            createdAt = 0,
            updatedAt = 0,
            resumes = listOf(VacancyResume(1)),
        )
    private suspend fun setup(): Long {
        dependencies.resumesRepository.records.value = listOf(resume)
        dependencies.vacanciesRepository.records.value = listOf(vacancy)
        dependencies.letterResponse = { CoverLetterGenerationOutcome.Generated("Dear team") }
        return dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Work",
                apiKey = "key",
                modelId = "model",
            ),
        )
    }

    @Test
    fun usesYearMonthDayOrderForGeneratedLetterNames() = runBlocking<Unit> {
        val connection = setup()
        val time = java.time.LocalDateTime.of(2026, 10, 20, 21, 10)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val useCase = GenerateCoverLetterUseCase(
            reframecv.coverletters.CoverLetterGenerationRepository { _, _, _, _ ->
                CoverLetterGenerationOutcome.Generated("Dear team")
            },
            dependencies.providerSettingsUseCases,
            dependencies.coverLettersRepository,
            timestamp = { time },
        )
        val result = useCase(resume, vacancy, connection, "Write")
        assertEquals("Cover letter: Main + ExampleCo (AI 2026-10-20 21:10)", result.name)
    }

    @Test
    fun savesAndLinksExistingVacancyWithoutOtherDraftChangesAndExcludesPrivateResumeData() =
        runBlocking {
            val id = setup()
            val result = dependencies.generateCoverLetter(
                resume,
                vacancy.copy(name = "Draft name"),
                id,
                "Write",
            )
            assertEquals(1L, result.sourceResumeId)
            assertEquals("Dear team", result.content)
            val stored = dependencies.vacanciesRepository.records.value.single()
            assertEquals("ExampleCo", stored.name)
            assertEquals(listOf(result.id), stored.coverLetterIds)
            val input = dependencies.letterInputs.single().first
            assertFalse(input.contains("secret.jpg"))
            assertFalse(input.contains("private"))
        }

    @Test
    fun newVacancyKeepsOnlyDraftLinkUntilSaveAndErrorsNeverCreateLetters() = runBlocking<Unit> {
        val id = setup()
        dependencies.generateCoverLetter(resume, vacancy.copy(id = 0), id, "Write")
        assertEquals(
            emptyList(),
            dependencies.vacanciesRepository.records.value.single().coverLetterIds,
        )
        assertEquals(1, dependencies.coverLettersRepository.records.value.size)
        dependencies.letterResponse = { CoverLetterGenerationOutcome.Generated(" ") }
        assertFailsWith<CoverLetterGenerationException> {
            dependencies.generateCoverLetter(resume, vacancy, id, "Write")
        }
        dependencies.letterResponse = { CoverLetterGenerationOutcome.Failed("Rejected") }
        assertEquals(
            "Rejected",
            assertFailsWith<CoverLetterGenerationException> {
                dependencies.generateCoverLetter(resume, vacancy, id, "Write")
            }.message,
        )
        dependencies.letterResponse = { throw CancellationException("Closed") }
        assertFailsWith<CancellationException> {
            dependencies.generateCoverLetter(resume, vacancy, id, "Write")
        }
        assertEquals(1, dependencies.coverLettersRepository.records.value.size)
    }

    @Test
    fun rejectsForeignOrUnlinkedSourcesAndDisabledConnections() = runBlocking<Unit> {
        val id = setup()
        assertFailsWith<IllegalArgumentException> {
            dependencies.generateCoverLetter(resume.copy(projectId = 8), vacancy, id, "Write")
        }
        assertFailsWith<IllegalArgumentException> {
            dependencies.generateCoverLetter(
                resume,
                vacancy.copy(resumes = emptyList()),
                id,
                "Write",
            )
        }
        dependencies.letterResponse =
            {
                dependencies.providerSettingsUseCases.setActive(id, false)
                CoverLetterGenerationOutcome.Generated("Text")
            }
        assertFailsWith<Exception> {
            dependencies.generateCoverLetter(resume, vacancy, id, "Write")
        }
        assertEquals(emptyList(), dependencies.coverLettersRepository.records.value)
    }
}
