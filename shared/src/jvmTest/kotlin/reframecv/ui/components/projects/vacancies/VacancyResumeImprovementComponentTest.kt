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
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.vacancy.Vacancy
import reframecv.improvement.ResumeImprovementOutcome
import reframecv.testing.ComponentTest
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.threading.runOnUiThread

class VacancyResumeImprovementComponentTest : ComponentTest() {
    private val record =
        Vacancy(
            id = 3,
            projectId = 7,
            name = "Stored",
            createdAt = 0,
            updatedAt = 0,
            description = "Kotlin",
        )
    private val content = ResumeContent(sections = listOf(ResumeSummarySection(text = "Improved")))

    private suspend fun editor(existing: Boolean): DefaultVacancyEditorComponent {
        dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Work",
                apiKey = "key",
                modelId = "model",
            ),
        )
        dependencies.promptsUseCases.save(
            Prompt(text = "Improve", component = PromptComponent.Vacancies),
        )
        dependencies.resumesRepository.records.value = listOf(Resume(1, 7, "Source", 0, 2))
        if (existing) dependencies.vacanciesRepository.records.value = listOf(record)
        dependencies.improvementResponse = { ResumeImprovementOutcome.Improved(content) }
        lateinit var editor: DefaultVacancyEditorComponent
        val ready = CompletableDeferred<Unit>()
        runOnUiThread {
            lifecycle.resume()
            editor =
                DefaultVacancyEditorComponent(appComponentContext(), save = {
                }, close = {}, initialVacancy = record.takeIf { existing }, projectId = 7)
            editor.onDraftChange(VacancyDraft("Unsaved title", description = "Kotlin"))
            editor.resumes.state.subscribe { if (it.available.isNotEmpty()) ready.complete(Unit) }
        }
        withTimeout(5000) { ready.await() }
        runOnUiThread { editor.resumes.onSelect(1) }
        return editor
    }

    private suspend fun open(
        editor: DefaultVacancyEditorComponent,
        id: Long = 1,
    ): AiRequestDialogComponent {
        lateinit var dialog: AiRequestDialogComponent
        val ready = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.improvement.onImprove(id)
            dialog = assertNotNull(editor.improvement.dialog.value.component)
            dialog.state.subscribe { if (it.canRun) ready.complete(Unit) }
        }
        withTimeout(5000) { ready.await() }
        return dialog
    }

    private suspend fun waitForLinks(editor: DefaultVacancyEditorComponent, count: Int) {
        val ready = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.draft.subscribe {
                if (it.resumes.size ==
                    count
                ) {
                    ready.complete(Unit)
                }
            }
        }
        withTimeout(5000) { ready.await() }
    }

    @Test
    fun successImmediatelyAddsSavedResumeAndAllowsAnotherImprovementCycle() = runBlocking {
        val editor = editor(true)
        val dialog = open(editor)
        runOnUiThread {
            assertTrue(editor.isBusy)
            editor.onDraftChange(editor.draft.value.copy(name = "Must not change"))
            assertEquals("Unsaved title", editor.draft.value.name)
            dialog.onRun()
            dialog.onRun()
        }
        waitForLinks(editor, 2)
        val improved = runOnUiThread {
            assertNull(editor.improvement.dialog.value.component)
            val link = editor.draft.value.resumes.last()
            assertEquals(0, link.atsScore)
            assertNull(link.assessment)
            assertTrue(editor.resumes.state.value.available.any { it.id == link.resumeId })
            assertEquals("Stored", dependencies.vacanciesRepository.records.value.single().name)
            assertEquals(
                link,
                dependencies.vacanciesRepository.records.value.single().resumes.last(),
            )
            link.resumeId
        }
        val next = open(editor, improved)
        runOnUiThread { next.onRun() }
        waitForLinks(editor, 3)
        assertEquals(improved, dependencies.resumesRepository.records.value.last().sourceResumeId)
        assertEquals(2, dependencies.improvementInputs.size)
    }

    @Test
    fun failureKeepsDialogAndRetryForNewVacancySavesOnlyResume() = runBlocking {
        val editor = editor(false)
        dependencies.improvementResponse =
            { ResumeImprovementOutcome.Failed("Invalid resume JSON") }
        val dialog = open(editor)
        val failed = CompletableDeferred<Unit>()
        runOnUiThread {
            dialog.state.subscribe { if (it.error != null) failed.complete(Unit) }
            dialog.onRun()
        }
        withTimeout(5000) { failed.await() }
        runOnUiThread {
            assertEquals(dialog, editor.improvement.dialog.value.component)
            assertEquals("Invalid resume JSON", dialog.state.value.error)
            assertEquals(1, dependencies.resumesRepository.records.value.size)
            dependencies.improvementResponse = { ResumeImprovementOutcome.Improved(content) }
            dialog.onText("Revised prompt")
            dialog.onRun()
        }
        waitForLinks(editor, 2)
        runOnUiThread { editor.onClose() }
        assertEquals(2, dependencies.resumesRepository.records.value.size)
        assertTrue(dependencies.vacanciesRepository.records.value.isEmpty())
    }

    @Test
    fun backClosesOnlyDialogAndLateCancelledResponseDoesNotCreateResume() = runBlocking {
        val editor = editor(true)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        dependencies.improvementResponse = {
            withContext(NonCancellable) {
                entered.complete(Unit)
                release.await()
                returned.complete(Unit)
                ResumeImprovementOutcome.Improved(content)
            }
        }
        val dialog = open(editor)
        runOnUiThread { dialog.onRun() }
        withTimeout(5000) { entered.await() }
        runOnUiThread {
            editor.onClose()
            assertNull(editor.improvement.dialog.value.component)
            assertEquals(1, editor.draft.value.resumes.size)
        }
        release.complete(Unit)
        withTimeout(5000) { returned.await() }
        runOnUiThread {
            assertEquals(1, dependencies.resumesRepository.records.value.size)
            assertEquals(1, editor.draft.value.resumes.size)
            assertTrue(dependencies.vacanciesRepository.records.value.single().resumes.isEmpty())
        }
    }
}
