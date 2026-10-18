package reframecv.database.vacancy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.recognition.RecognizedVacancy
import reframecv.recognition.applyTo
import reframecv.repository.LocalVacanciesRepository
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft

class VacancyStructuredContentTest : WorkspaceDatabaseTest() {
    @Test
    fun recognizedContentSurvivesDatabaseReopenDraftEditingAndKeywordSearch() = runBlocking {
        val projectId = project()
        val repository = LocalVacanciesRepository(database.vacancyDao())
        val recognized = RecognizedVacancy(
            jobTitle = "Mobile developer",
            description = "Build mobile apps",
            company = "Recognized company",
            url = "https://example.com/recognized",
            responsibilities = listOf("Build apps, APIs", "Work with teams\nReview code"),
            requirements = listOf("Kotlin"),
            niceToHave = listOf("CI/CD"),
        ).applyTo(vacancy(projectId).copy(keywords = listOf("Kotlin", "Android")))
        val id = repository.create(recognized)
        database.close()
        openDatabase()
        val reopened = LocalVacanciesRepository(database.vacancyDao())
        val saved = reopened.observeVacancies(projectId).first().single()
        assertEquals(id, saved.id)
        assertEquals(recognized.name, saved.name)
        assertEquals(listOf("Kotlin", "Android"), saved.keywords)
        assertEquals(recognized.jobTitle, saved.jobTitle)
        assertEquals(recognized.url, saved.url)
        assertEquals(recognized.responsibilities, saved.responsibilities)
        assertEquals(recognized.requirements, saved.requirements)
        assertEquals(recognized.niceToHave, saved.niceToHave)
        assertEquals(recognized.company, saved.company)
        assertEquals(listOf(saved), reopened.search(projectId, " KOTLIN "))
        val draft = VacancyDraft.fromVacancy(saved).copy(name = "Edited")
        reopened.update(draft.applyTo(saved))
        val edited = reopened.observeVacancies(projectId).first().single()
        assertEquals("Edited", edited.name)
        assertEquals(saved.jobTitle, edited.jobTitle)
        assertEquals(saved.responsibilities, edited.responsibilities)
        assertEquals(saved.requirements, edited.requirements)
        assertEquals(saved.niceToHave, edited.niceToHave)
        assertEquals(saved.responsibilities, draft.toVacancy(projectId).responsibilities)
    }

    @Test
    fun emptyListsAndUnicodeProseAreStoredWithoutCommaSplitting() = runBlocking {
        val repository = LocalVacanciesRepository(database.vacancyDao())
        val id = repository.create(vacancy(project()))
        val empty = repository.observeVacancies(
            database.vacancyDao().findById(id)!!.entity.projectId,
        )
            .first().single()
        assertEquals(emptyList(), empty.responsibilities)
        assertEquals(emptyList(), empty.requirements)
        assertEquals(emptyList(), empty.niceToHave)
        val items = listOf("Résumé, Entwicklung", "Line 1\nLine 2", "SQL \"queries\"", "東京")
        repository.update(
            empty.copy(responsibilities = items, requirements = items, niceToHave = items),
        )
        val record = checkNotNull(database.vacancyDao().findById(id)).toDomainModel()
        assertEquals(items, record.responsibilities)
        assertEquals(items, record.requirements)
        assertEquals(items, record.niceToHave)
    }

    @Test
    fun converterRejectsMalformedAndNonStringLists() {
        val converter = VacancyListConverter()
        listOf("null", "{}", "42", "[null]", "[42]", "[true]", "broken").forEach {
            assertFailsWith<IllegalArgumentException>(it) { converter.decode(it) }
        }
        assertEquals(
            listOf("A, B", "C\nD"),
            converter.decode(converter.encode(listOf("A, B", "C\nD"))),
        )
    }
}
