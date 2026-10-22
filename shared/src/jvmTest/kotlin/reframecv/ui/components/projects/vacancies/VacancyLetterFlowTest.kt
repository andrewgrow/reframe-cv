package reframecv.ui.components.projects.vacancies

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import reframecv.coverletters.CoverLetterGenerationOutcome
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.ComponentTest
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.threading.runOnUiThread

class VacancyLetterFlowTest : ComponentTest() {
    private val vacancy =
        Vacancy(
            id = 3,
            projectId = 7,
            name = "ExampleCo",
            createdAt = 0,
            updatedAt = 0,
            resumes = listOf(VacancyResume(1), VacancyResume(2)),
        )
    private suspend fun editor(target: Vacancy = vacancy): DefaultVacancyEditorComponent {
        dependencies.resumesRepository.records.value =
            listOf(Resume(1, 7, "Main", 0, 0), Resume(2, 7, "Tailored", 0, 0))
        dependencies.vacanciesRepository.records.value = listOf(vacancy)
        dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Work",
                configuration = OpenRouterConfiguration(
                    apiKey = "key",
                    modelId = "model",
                ),
            ),
        )
        dependencies.promptsUseCases.save(
            Prompt(text = "Write", component = PromptComponent.CoverLetters),
        )
        dependencies.promptsUseCases.save(
            Prompt(text = "Wrong scope", component = PromptComponent.Vacancies),
        )
        val ready = CompletableDeferred<Unit>()
        val editor = runOnUiThread {
            lifecycle.resume()
            DefaultVacancyEditorComponent(appComponentContext(), {
            }, {}, initialVacancy = target).also { component ->
                component.resumes.state.subscribe {
                    if (it.available.size ==
                        2
                    ) {
                        ready.complete(Unit)
                    }
                }
            }
        }
        withTimeout(5000) { ready.await() }
        return editor
    }
    private suspend fun ready(dialog: AiRequestDialogComponent) {
        val loaded = CompletableDeferred<Unit>()
        runOnUiThread { dialog.state.subscribe { if (!it.loading) loaded.complete(Unit) } }
        withTimeout(5000) { loaded.await() }
    }

    @Test
    fun choosesResumeFiltersPromptsAndImmediatelyAddsGeneratedLetter() = runBlocking<Unit> {
        val editor = editor()
        dependencies.letterResponse = { CoverLetterGenerationOutcome.Generated("Dear team") }
        val created = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.letterGeneration.onGenerate()
            assertTrue(editor.letterGeneration.selection.value.visible)
            editor.letterGeneration.onResume(2)
            editor.letterGeneration.onContinue()
            editor.draft.subscribe { if (it.coverLetterIds.isNotEmpty()) created.complete(Unit) }
        }
        val dialog = editor.letterGeneration.dialog.value.component!!
        ready(dialog)
        runOnUiThread {
            assertEquals(listOf("Write"), dialog.state.value.prompts.map { it.text })
            dialog.onRun()
        }
        withTimeout(5000) { created.await() }
        runOnUiThread {
            assertNull(editor.letterGeneration.dialog.value.component)
            assertEquals(listOf(1L), editor.draft.value.coverLetterIds)
            assertEquals(
                2L,
                dependencies.coverLettersRepository.records.value.single().sourceResumeId,
            )
            assertEquals(
                listOf(1L),
                dependencies.vacanciesRepository.records.value.single().coverLetterIds,
            )
        }
    }

    @Test
    fun autoSelectsSingleResumeAndCancellationDiscardsLateResults() = runBlocking<Unit> {
        val editor = editor(vacancy.copy(resumes = listOf(VacancyResume(1))))
        val response = CompletableDeferred<CoverLetterGenerationOutcome>()
        val started = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        dependencies.letterResponse = {
            started.complete(Unit)
            withContext(NonCancellable) { response.await() }.also { returned.complete(Unit) }
        }
        runOnUiThread { editor.letterGeneration.onGenerate() }
        assertFalse(editor.letterGeneration.selection.value.visible)
        val dialog = editor.letterGeneration.dialog.value.component!!
        ready(dialog)
        runOnUiThread { dialog.onRun() }
        withTimeout(5000) { started.await() }
        runOnUiThread { editor.onClose() }
        response.complete(CoverLetterGenerationOutcome.Generated("Too late"))
        withTimeout(5000) { returned.await() }
        runOnUiThread {
            assertNull(editor.letterGeneration.dialog.value.component)
            assertTrue(dependencies.coverLettersRepository.records.value.isEmpty())
        }
    }

    @Test
    fun restoresBothDocumentBlocksWithoutChangingTheirSavedValues() = runBlocking<Unit> {
        val owner = reframecv.domain.models.screenstate.ScreenStateOwner(
            reframecv.domain.models.screenstate.ScreenType.Vacancy,
            3,
            7,
        )
        dependencies.screenStatesRepository.states.value =
            mapOf(
                owner to kotlinx.serialization.json.buildJsonObject {
                    put("linked_resumes", kotlinx.serialization.json.JsonPrimitive("collapsed"))
                    put("linked_letters", kotlinx.serialization.json.JsonPrimitive("expanded"))
                },
            )
        val editor = editor()
        val restored = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.resumeSection.state.subscribe { if (!it.loading) restored.complete(Unit) }
        }
        withTimeout(5000) { restored.await() }
        runOnUiThread {
            assertFalse(editor.resumeSection.state.value.expanded)
            editor.letterSection.onToggle()
        }
        val saved = dependencies.screenStateUseCases.load(owner)!!
        assertEquals("collapsed", saved.text("linked_resumes"))
        assertEquals("collapsed", saved.text("linked_letters"))
    }

    @Test
    fun canAttachReuseUnlinkAndOpenExistingLettersWithoutDeletingThem() = runBlocking<Unit> {
        dependencies.coverLettersRepository.records.value =
            listOf(CoverLetter(10, 7, "Reusable", 0, 0))
        val editor = editor()
        val loaded = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.letters.state.subscribe { if (it.available.isNotEmpty()) loaded.complete(Unit) }
        }
        withTimeout(5000) { loaded.await() }
        runOnUiThread {
            editor.letters.onSelect(10)
            editor.letters.onSelect(10)
            editor.letters.onSelect(999)
            assertEquals(listOf(10L), editor.draft.value.coverLetterIds)
            editor.letters.onUnlink(10)
            editor.onClose()
            assertNull(editor.letters.state.value.unlinkId)
            editor.letters.onUnlink(10)
            editor.letters.onConfirmUnlink()
            assertEquals(emptyList(), editor.draft.value.coverLetterIds)
            assertEquals(1, dependencies.coverLettersRepository.records.value.size)
        }
    }
}
