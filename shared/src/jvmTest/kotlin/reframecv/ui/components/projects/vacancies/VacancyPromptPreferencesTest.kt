package reframecv.ui.components.projects.vacancies

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.ComponentTest
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.threading.runOnUiThread

class VacancyPromptPreferencesTest : ComponentTest() {
    private suspend fun editor(record: Vacancy? = null): DefaultVacancyEditorComponent {
        lateinit var editor: DefaultVacancyEditorComponent
        val ready = CompletableDeferred<Unit>()
        runOnUiThread {
            editor =
                DefaultVacancyEditorComponent(appComponentContext(), save = {
                }, close = {}, initialVacancy = record, projectId = 7)
            if (record ==
                null
            ) {
                editor.onDraftChange(
                    VacancyDraft(
                        "Draft",
                        description = "Kotlin",
                        resumes = listOf(VacancyResume(1)),
                    ),
                )
            }
            editor.resumes.state.subscribe { if (!it.loading) ready.complete(Unit) }
        }
        withTimeout(5000) { ready.await() }
        return editor
    }

    private suspend fun open(
        editor: DefaultVacancyEditorComponent,
        improvement: Boolean,
    ): AiRequestDialogComponent {
        lateinit var dialog: AiRequestDialogComponent
        val ready = CompletableDeferred<Unit>()
        runOnUiThread {
            if (improvement) editor.improvement.onImprove(1) else editor.assessment.onAssess(1)
            val slot = if (improvement) editor.improvement.dialog else editor.assessment.dialog
            dialog = assertNotNull(slot.value.component)
            dialog.state.subscribe { if (!it.loading) ready.complete(Unit) }
        }
        withTimeout(5000) { ready.await() }
        return dialog
    }

    private suspend fun use(dialog: AiRequestDialogComponent, prompt: Long) {
        val failed = CompletableDeferred<Unit>()
        runOnUiThread {
            dialog.onPrompt(prompt)
            dialog.state.subscribe { if (it.error != null) failed.complete(Unit) }
            dialog.onRun()
        }
        withTimeout(5000) { failed.await() }
        runOnUiThread { dialog.onClose() }
    }

    @Test
    fun draftRemembersBothOperationsAndFirstSavePersistsThemForReopenedPage() = runBlocking {
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
            Prompt(text = "Assess", component = PromptComponent.Vacancies),
        )
        dependencies.promptsUseCases.save(
            Prompt(text = "Improve", component = PromptComponent.Vacancies),
        )
        dependencies.resumesRepository.records.value = listOf(Resume(1, 7, "Resume", 0, 0))
        val draft = editor()
        use(open(draft, improvement = false), 1)
        use(open(draft, improvement = true), 2)
        val again = open(draft, improvement = false)
        runOnUiThread {
            assertEquals(1L, again.state.value.promptId)
            again.onClose()
        }
        assertTrue(dependencies.screenStatesRepository.states.value.isEmpty())
        val saved =
            Vacancy(
                id = 12,
                projectId = 7,
                name = "Saved",
                description = "Kotlin",
                createdAt = 0,
                updatedAt = 0,
                resumes = listOf(VacancyResume(1)),
            )
        runOnUiThread { draft.onSaveCompleted(VacancySaveState.Idle, saved) }
        val stored = dependencies.screenStateUseCases.load(
            ScreenStateOwner(ScreenType.Vacancy, 12, 7),
        )!!
        assertEquals(JsonPrimitive(1L), stored.states["resume_assessment_prompt_id"])
        assertEquals(JsonPrimitive(2L), stored.states["resume_improvement_prompt_id"])
        assertEquals("expanded", stored.text("job_details"))
        val reopened = editor(saved)
        val assessment = open(reopened, improvement = false)
        runOnUiThread {
            assertEquals(1L, assessment.state.value.promptId)
            assessment.onClose()
        }
        val improvement = open(reopened, improvement = true)
        runOnUiThread { assertEquals(2L, improvement.state.value.promptId) }
    }
}
