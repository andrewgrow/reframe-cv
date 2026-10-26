package reframecv.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.ComponentTest
import reframecv.usecase.VacancyArchiveUseCases

class VacancyArchiveUseCasesTest : ComponentTest() {
    private val vacancy = Vacancy(
        7,
        1,
        "Example role",
        0,
        0,
        resumes = listOf(VacancyResume(2), VacancyResume(1), VacancyResume(99)),
        coverLetterIds = listOf(2, 1, 99),
    )
    private fun populate() {
        dependencies.resumesRepository.records.value = listOf(
            Resume(2, 1, "Second", 0, 0),
            Resume(1, 1, "First", 0, 0),
            Resume(3, 1, "Unlinked", 0, 0),
            Resume(99, 2, "Other project", 0, 0),
        )
        dependencies.coverLettersRepository.records.value = listOf(
            CoverLetter(2, 1, "Second", 0, 0),
            CoverLetter(1, 1, "First", 0, 0),
            CoverLetter(3, 1, "Unlinked", 0, 0),
            CoverLetter(99, 2, "Other project", 0, 0),
        )
    }

    @Test
    fun choicesAndExportUseOnlyLinkedProjectDocumentsInStableIdOrder() = runBlocking {
        populate()
        var captured: VacancyArchive? = null
        val cases = VacancyArchiveUseCases(
            dependencies.resumesRepository,
            dependencies.coverLettersRepository,
            VacancyArchiveExporter {
                captured = it
                DocumentExportResult.Saved
            },
        )
        val choices = cases.choices(vacancy)
        assertEquals(listOf(1L, 2L), choices.resumes.map { it.id })
        assertEquals(listOf(1L, 2L), choices.coverLetters.map { it.id })
        assertEquals(
            DocumentExportResult.Saved,
            cases.export(vacancy, "  Example / role  ", setOf(2, 1), setOf(2)),
        )
        assertEquals("Example - role", captured?.name)
        assertEquals(listOf(1L, 2L), captured?.resumes?.map { it.id })
        assertEquals(listOf(2L), captured?.coverLetters?.map { it.id })
    }

    @Test
    fun staleUnlinkedOrForeignSelectionsFailBeforeAnyArchiveIsGenerated() = runBlocking {
        populate()
        var exports = 0
        val cases = VacancyArchiveUseCases(
            dependencies.resumesRepository,
            dependencies.coverLettersRepository,
            VacancyArchiveExporter {
                exports++
                DocumentExportResult.Saved
            },
        )
        assertFailsWith<IllegalArgumentException> {
            cases.export(vacancy, "Job", setOf(3), emptySet())
        }
        assertFailsWith<IllegalArgumentException> {
            cases.export(vacancy, "Job", emptySet(), setOf(99))
        }
        dependencies.resumesRepository.records.value = emptyList()
        assertFailsWith<IllegalArgumentException> {
            cases.export(vacancy, "Job", setOf(1), emptySet())
        }
        assertFailsWith<IllegalArgumentException> {
            cases.export(vacancy, " ... ", emptySet(), emptySet())
        }
        assertEquals(0, exports)
        cases.export(vacancy, "Job", emptySet(), emptySet())
        assertEquals(1, exports)
    }

    @Test
    fun filePrefixCannotIntroduceZipDirectoriesOrControlCharacters() {
        assertEquals("-Name---file", archiveFilePrefix(" ../Name/\\:file. "))
        assertEquals("", archiveFilePrefix(" .. "))
        assertEquals("Role-name", archiveFilePrefix("Role\nname"))
        assertEquals("Вакансия", archiveFilePrefix("Вакансия"))
        val long = archiveFilePrefix("Long role ".repeat(30))
        assertEquals(long, archiveFilePrefix(long))
    }
}
