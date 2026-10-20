package reframecv.usecase

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.improvement.ResumeImprovementOutcome
import reframecv.improvement.ResumeImprovementRepository
import reframecv.testing.ComponentTest

class ImproveResumeUseCaseTest : ComponentTest() {
    private val source =
        Resume(
            1,
            7,
            "main resume",
            1,
            2,
            keywords = listOf("private keyword"),
            photoPath = "private/photo.jpg",
        )
    private val vacancy =
        Vacancy(
            id = 3,
            projectId = 7,
            name = "Stored",
            createdAt = 1,
            updatedAt = 2,
            description = "Kotlin",
            resumes = listOf(VacancyResume(1, 53)),
        )
    private val content =
        ResumeContent(sections = listOf(ResumeSummarySection(text = "Relevant Android experience")))

    private suspend fun setup(): Long {
        dependencies.resumesRepository.records.value = listOf(source)
        dependencies.resumesRepository.mainResumeIds.value = mapOf(7L to 1L)
        dependencies.vacanciesRepository.records.value = listOf(vacancy)
        dependencies.improvementResponse = { ResumeImprovementOutcome.Improved(content) }
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
    fun adaptationUsesLocalTimestampAndKeepsSourceWithoutCopyingItsPhotoOrKeywords() = runBlocking {
        val connection = setup()
        val time = LocalDateTime.of(
            2026,
            10,
            20,
            18,
            10,
        ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val useCase = ImproveResumeUseCase(
            ResumeImprovementRepository { _, _, _, _ ->
                ResumeImprovementOutcome.Improved(content)
            },
            dependencies.providerSettingsUseCases,
            dependencies.resumesRepository,
            timestamp = { time },
        )
        val result = useCase(source, vacancy.copy(name = "Unsaved draft"), connection, "Improve")
        assertEquals("main resume + Unsaved draft (AI 2026-20-10 18:10)", result.name)
        assertEquals(source.id, result.sourceResumeId)
        assertEquals(content, result.content)
        assertEquals(emptyList(), result.keywords)
        assertNull(result.photoPath)
        assertEquals(source, dependencies.resumesRepository.records.value.first())
        assertEquals(1L, dependencies.resumesRepository.mainResumeIds.value[7])
        val stored = dependencies.vacanciesRepository.records.value.single()
        assertEquals("Stored", stored.name)
        assertEquals(listOf(VacancyResume(1, 53), VacancyResume(result.id)), stored.resumes)
    }

    @Test
    fun repeatedImprovementKeepsOneVacancyNameAndReplacesAiTimestamps() = runBlocking {
        val connection = setup()
        val time = LocalDateTime.of(2026, 10, 20, 21, 10)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val useCase = ImproveResumeUseCase(
            ResumeImprovementRepository { _, _, _, _ ->
                ResumeImprovementOutcome.Improved(content)
            },
            dependencies.providerSettingsUseCases,
            dependencies.resumesRepository,
            timestamp = { time },
        )
        listOf(
            "main resume (AI 2026-20-10 20:41)",
            "main resume (AI 2026-20-10 20:41) (AI 2026-20-10 20:50)",
            "main resume + Stored (AI 2026-20-10 20:41)",
            "main resume + Stored + Stored (AI 2026-20-10 20:41)",
            "main resume + Stored",
        ).forEach { name ->
            val result = useCase(source.copy(name = name), vacancy, connection, "Improve")
            assertEquals("main resume + Stored (AI 2026-20-10 21:10)", result.name)
        }
        val custom = "AI specialist (AI draft)"
        val result = useCase(source.copy(name = custom), vacancy, connection, "Improve")
        assertEquals("$custom + Stored (AI 2026-20-10 21:10)", result.name)
        val named = vacancy.copy(name = " Kontron (EU) + Mobile ")
        val special =
            useCase(source.copy(name = "main + archive"), named, connection, "Improve")
        assertEquals(
            "main + archive + Kontron (EU) + Mobile (AI 2026-20-10 21:10)",
            special.name,
        )
        val draft = useCase(source, vacancy.copy(id = 0, name = ""), connection, "Improve")
        assertEquals("main resume (AI 2026-20-10 21:10)", draft.name)
    }

    @Test
    fun draftVacancyKeepsNewResumeInProjectAndSendsOnlyPortableContent() = runBlocking {
        val connection = setup()
        dependencies.vacanciesRepository.records.value = emptyList()
        val result = dependencies.improveResume(source, vacancy.copy(id = 0), connection, "Improve")
        assertEquals(2, dependencies.resumesRepository.records.value.size)
        assertEquals(source.id, result.sourceResumeId)
        assertTrue(dependencies.vacanciesRepository.records.value.isEmpty())
        val inputs = dependencies.improvementInputs.single()
        assertFalse(inputs.first.contains("private/photo.jpg"))
        assertFalse(inputs.first.contains("private keyword"))
        assertFalse(inputs.first.contains("sourceResumeId"))
        assertTrue(inputs.second.contains("Kotlin"))
    }

    @Test
    fun failureCancellationAndMissingVacancyNeverCreateAResume() = runBlocking {
        val connection = setup()
        dependencies.improvementResponse = { ResumeImprovementOutcome.Failed("API key expired.") }
        val error = assertFailsWith<ResumeImprovementException> {
            dependencies.improveResume(source, vacancy, connection, "Improve")
        }
        assertEquals("API key expired.", error.message)
        dependencies.improvementResponse = { throw CancellationException("Closed") }
        assertFailsWith<CancellationException> {
            dependencies.improveResume(source, vacancy, connection, "Improve")
        }
        dependencies.improvementResponse = { ResumeImprovementOutcome.Improved(content) }
        dependencies.vacanciesRepository.records.value = emptyList()
        val persistence = assertFailsWith<ResumeImprovementException> {
            dependencies.improveResume(source, vacancy, connection, "Improve")
        }
        assertEquals("The improved resume could not be saved. Please retry.", persistence.message)
        assertEquals(listOf(source), dependencies.resumesRepository.records.value)
    }

    @Test
    fun guardsRejectMissingDetailsWrongProjectAndProviderDisabledDuringGeneration() = runBlocking {
        val connection = setup()
        assertFailsWith<ResumeImprovementException> {
            dependencies.improveResume(
                source,
                vacancy.copy(description = ""),
                connection,
                "Improve",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            dependencies.improveResume(source.copy(projectId = 8), vacancy, connection, "Improve")
        }
        assertFailsWith<IllegalArgumentException> {
            dependencies.improveResume(
                source,
                vacancy.copy(resumes = emptyList()),
                connection,
                "Improve",
            )
        }
        dependencies.improvementResponse = {
            dependencies.providerSettingsUseCases.setActive(connection, false)
            ResumeImprovementOutcome.Improved(content)
        }
        assertFailsWith<Exception> {
            dependencies.improveResume(source, vacancy, connection, "Improve")
        }
        assertEquals(listOf(source), dependencies.resumesRepository.records.value)
    }
}
