package reframecv.ui.components.projects.vacancies

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import reframecv.assessment.ResumeAssessmentOutcome
import reframecv.assessment.ResumeAssessmentResult
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.ComponentTest
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.threading.runOnUiThread

class VacancyResumeAssessmentComponentTest : ComponentTest() {
    private val saved = mutableListOf<VacancyDraft>()
    private val record =
        Vacancy(
            id = 3,
            projectId = 7,
            name = "Stored",
            description = "Kotlin required",
            createdAt = 0,
            updatedAt = 0,
        )

    private suspend fun editor(existing: Boolean): DefaultVacancyEditorComponent {
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
        dependencies.resumesRepository.records.value = listOf(Resume(1, 7, "CV", 0, 2))
        if (existing) dependencies.vacanciesRepository.records.value = listOf(record)
        lateinit var editor: DefaultVacancyEditorComponent
        val ready = CompletableDeferred<Unit>()
        runOnUiThread {
            lifecycle.resume()
            editor =
                DefaultVacancyEditorComponent(appComponentContext(), save = {
                    saved += it
                }, close = {}, initialVacancy = record.takeIf { existing }, projectId = 7)
            editor.onDraftChange(VacancyDraft("Unsaved title", description = "Kotlin required"))
            editor.resumes.state.subscribe { if (it.available.isNotEmpty()) ready.complete(Unit) }
        }
        withTimeout(5000) { ready.await() }
        runOnUiThread { editor.resumes.onSelect(1) }
        return editor
    }

    private suspend fun open(editor: DefaultVacancyEditorComponent): AiRequestDialogComponent {
        val ready = CompletableDeferred<Unit>()
        lateinit var dialog: AiRequestDialogComponent
        runOnUiThread {
            editor.assessment.onAssess(1)
            dialog = assertNotNull(editor.assessment.dialog.value.component)
            dialog.state.subscribe { if (it.canRun) ready.complete(Unit) }
        }
        withTimeout(5000) { ready.await() }
        return dialog
    }

    private suspend fun result(editor: DefaultVacancyEditorComponent) {
        val done = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.assessment.selection.subscribe {
                if (it.resumeId !=
                    null
                ) {
                    done.complete(Unit)
                }
            }
        }
        withTimeout(5000) { done.await() }
    }

    @Test
    fun existingAssessmentSavesImmediatelyButDoesNotSaveOtherEditorFields() = runBlocking {
        var requests = 0
        dependencies.assessmentResponse =
            {
                requests++
                ResumeAssessmentOutcome.Assessed(ResumeAssessmentResult(84, "Strong Kotlin match."))
            }
        val editor = editor(true)
        val dialog = open(editor)
        assertEquals(0, requests)
        runOnUiThread { dialog.onRun() }
        result(editor)
        runOnUiThread {
            assertNull(editor.assessment.dialog.value.component)
            assertEquals(84, editor.draft.value.resumes.single().atsScore)
            assertEquals("Stored", dependencies.vacanciesRepository.records.value.single().name)
            assertEquals(
                84,
                dependencies.vacanciesRepository.records.value.single().resumes.single().atsScore,
            )
            assertTrue(saved.isEmpty())
            editor.onClose()
            editor.onSave(editor.draft.value)
            assertEquals(84, saved.single().resumes.single().assessment?.result?.score)
        }
    }

    @Test
    fun errorsStayInDialogAndNewVacancyAssessmentWaitsForSave() = runBlocking {
        dependencies.assessmentResponse = { ResumeAssessmentOutcome.Failed("API key expired.") }
        val editor = editor(false)
        val dialog = open(editor)
        val failed = CompletableDeferred<Unit>()
        runOnUiThread {
            dialog.onRun()
            dialog.state.subscribe { if (it.error != null) failed.complete(Unit) }
        }
        withTimeout(5000) { failed.await() }
        runOnUiThread {
            assertEquals("API key expired.", dialog.state.value.error)
            assertEquals(dialog, editor.assessment.dialog.value.component)
            dependencies.assessmentResponse =
                {
                    ResumeAssessmentOutcome.Assessed(
                        ResumeAssessmentResult(75, "Relevant experience, some gaps."),
                    )
                }
            dialog.onText("Updated instruction")
            dialog.onRun()
        }
        result(editor)
        runOnUiThread {
            assertTrue(dependencies.vacanciesRepository.records.value.isEmpty())
            assertTrue(saved.isEmpty())
            editor.assessment.onCloseResult()
            editor.onSave(editor.draft.value)
            assertEquals(75, saved.single().resumes.single().assessment?.result?.score)
        }
    }

    @Test
    fun closingDialogCancelsRequestAndDiscardsEvenALateResult() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        dependencies.assessmentResponse = {
            entered.complete(Unit)
            withContext(NonCancellable) { release.await() }
            returned.complete(Unit)
            ResumeAssessmentOutcome.Assessed(ResumeAssessmentResult(90, "Late result."))
        }
        val editor = editor(true)
        val dialog = open(editor)
        runOnUiThread { dialog.onRun() }
        withTimeout(5000) { entered.await() }
        runOnUiThread { dialog.onClose() }
        release.complete(Unit)
        withTimeout(5000) { returned.await() }
        runOnUiThread {
            assertNull(editor.assessment.dialog.value.component)
            assertNull(editor.draft.value.resumes.single().assessment)
            assertTrue(dependencies.vacanciesRepository.records.value.single().resumes.isEmpty())
        }
    }
}
