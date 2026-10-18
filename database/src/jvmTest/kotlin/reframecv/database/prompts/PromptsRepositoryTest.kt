package reframecv.database.prompts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.repository.LocalPromptsRepository
import reframecv.usecase.PromptsUseCases

class PromptsRepositoryTest : WorkspaceDatabaseTest() {
    @Test
    fun startsEmptyAndPreservesUserEditsAndDisabledStatusAfterReopening() = runBlocking {
        var repository = LocalPromptsRepository(database.promptsDao())
        val lists = coroutineScope {
            val first = async { repository.observeAll().first() }
            val second = async { repository.observeAll().first() }
            listOf(first.await(), second.await())
        }
        assertEquals(emptyList(), lists.first())
        assertEquals(lists.first(), lists.last())
        assertEquals(emptyList(), repository.observeAvailable(PromptComponent.Vacancies).first())
        repository.save(
            Prompt(
                title = "My prompt",
                component = PromptComponent.Vacancies,
                text = "Extract facts.",
            ),
        )
        val original = repository.observeAll().first().single()
        val edited = original.copy(
            title = "My extraction",
            text = "Keep original language.",
            active = false,
        )
        assertEquals(original.id, repository.save(edited))
        database.close()
        openDatabase()
        repository = LocalPromptsRepository(database.promptsDao())
        val reopened = repository.observeAll().first().single()
        assertEquals(edited.text, reopened.text)
        assertEquals(edited.title, reopened.title)
        assertEquals(original.createdAt, reopened.createdAt)
        assertFalse(reopened.active)
        assertEquals(emptyList(), repository.observeAvailable(PromptComponent.Vacancies).first())
    }

    @Test
    fun componentChoicesIncludeEnabledGeneralPromptsAndReactToEdits() = runBlocking {
        val repository = LocalPromptsRepository(database.promptsDao())
        val general = repository.save(Prompt(text = "General instructions"))
        val resumes = repository.save(
            Prompt(component = PromptComponent.Resume, text = "Resume instructions"),
        )
        val vacancies = repository.save(
            Prompt(component = PromptComponent.Vacancies, text = "Vacancy instructions"),
        )
        assertEquals(
            listOf(general, vacancies),
            repository.observeAvailable(PromptComponent.Vacancies).first().map {
                it.id
            },
        )
        assertEquals(
            listOf(general, resumes),
            repository.observeAvailable(PromptComponent.Resume).first().map {
                it.id
            },
        )
        assertEquals(
            listOf(general),
            repository.observeAvailable(PromptComponent.CoverLetters).first().map {
                it.id
            },
        )
        val generalPrompt = repository.observeAll().first().first { it.id == general }
        repository.save(generalPrompt.copy(active = false))
        assertEquals(emptyList(), repository.observeAvailable(PromptComponent.Projects).first())
        assertEquals(3, repository.observeAll().first().size)
        assertEquals(
            listOf(general, resumes, vacancies),
            repository.observeAll().first().map {
                it.id
            },
        )
    }

    @Test
    fun validatesTextAllowsOptionalFieldsAndNeverInsertsAMissingUpdate() = runBlocking {
        val repository = LocalPromptsRepository(database.promptsDao())
        val useCases = PromptsUseCases(repository)
        repository.observeAll().first()
        assertFailsWith<IllegalArgumentException> { useCases.save(Prompt(text = " \n ")) }
        val id = useCases.save(
            Prompt(title = "   ", text = "  Instructions\n  Keep indentation.  "),
        )
        val prompt = repository.observeAll().first().last()
        assertEquals("", prompt.title)
        assertEquals(null, prompt.component)
        assertEquals("Instructions\n  Keep indentation.", prompt.text)
        assertEquals(id, useCases.save(prompt.copy(text = "Updated")))
        assertEquals(1, repository.observeAll().first().size)
        assertFailsWith<IllegalStateException> { repository.save(prompt.copy(id = 999)) }
        assertEquals(1, repository.observeAll().first().size)
    }
}
