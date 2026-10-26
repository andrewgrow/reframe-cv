package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerResult
import reframecv.relevance.RunnerRelevanceScore
import reframecv.runners.RunnerResultsPage
import reframecv.testing.ComponentTest
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.RunnerRunView

class RunnerRelevanceComponentTest : ComponentTest() {
    private val original = RunnerResult(
        1, 3, 0, "Example source", "101", """{"title":"Android developer","extra":true}""", 100,
        relevanceScore = 40, relevancePrompt = "Old criteria",
    )
    private val pageState = MutableValue(
        RunnerRunsState(
            RunnerDraft.newRunner(1).copy(id = 1, name = "Example search"),
            view = RunnerRunView(
                ConnectionRun(id = 3, runnerId = 1, configurationSnapshot = "{}", startedAt = 100),
                null,
                RunnerResultsPage(emptyList(), 0, null, savedResults = listOf(original)),
            ),
        ),
    )
    private suspend fun component(): RunnerRelevanceComponent {
        dependencies.runnerResultsRepository.records += original
        dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Example AI",
                configuration = OpenRouterConfiguration(
                    apiKey = "fictional-key",
                    modelId = "example/model",
                ),
            ),
        )
        dependencies.promptsUseCases.save(
            Prompt(text = "Find Android roles", component = PromptComponent.Vacancies),
        )
        return runOnUiThread {
            lifecycle.resume()
            DefaultRunnerRelevanceComponent(appComponentContext(), pageState) { true }
        }
    }
    private suspend fun <T : Any> await(value: Value<T>, check: (T) -> Boolean): T {
        val ready = CompletableDeferred<T>()
        val subscription = runOnUiThread { value.subscribe { if (check(it)) ready.complete(it) } }
        return try {
            withTimeout(5000) { ready.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }
    private suspend fun open(component: RunnerRelevanceComponent): AiRequestDialogComponent {
        runOnUiThread { component.assess(original.id) }
        val dialog = assertNotNull(component.dialog.value.component)
        await(dialog.state) { it.canRun }
        return dialog
    }
    private suspend fun error(dialog: AiRequestDialogComponent): AiRequestDialogState =
        await(dialog.state) { !it.running && it.error != null }

    @Test
    fun persistsScoreAndExactEditedPromptAndUpdatesTheVisibleRowWithoutImporting() =
        runBlocking<Unit> {
            val component = component()
            val dialog = open(component)
            dependencies.relevanceResponse = { source, instruction ->
                assertEquals(original.rawResultAsJson, source)
                assertEquals("Custom Android criteria", instruction)
                RunnerRelevanceScore(91)
            }
            runOnUiThread {
                dialog.onText("Custom Android criteria")
                dialog.onRun()
            }
            await(component.dialog) { it.component == null }
            val saved = dependencies.runnerResultsRepository.get(original.id)!!
            assertEquals(91, saved.relevanceScore)
            assertEquals("Custom Android criteria", saved.relevancePrompt)
            assertEquals(original.rawResultAsJson, saved.rawResultAsJson)
            assertEquals(original.status, saved.status)
            assertEquals(91, pageState.value.view?.page?.savedResults?.single()?.relevanceScore)
            runOnUiThread { component.view(original.id) }
            assertEquals(saved.relevancePrompt, component.selection.value.result?.relevancePrompt)
            runOnUiThread { assertTrue(component.onBack()) }
            assertNull(component.selection.value.result)
            assertEquals(0, dependencies.runnerExecutionRepository.starts)
        }

    @Test
    fun persistenceFailureRetainsPreviousScoreAndDialogForRetry() = runBlocking<Unit> {
        val component = component()
        val dialog = open(component)
        dependencies.relevanceResponse = { _, _ -> RunnerRelevanceScore(0) }
        dependencies.runnerResultsRepository.failRelevanceSave = true
        runOnUiThread { dialog.onRun() }
        assertTrue(error(dialog).error!!.contains("could not be saved"))
        assertNotNull(component.dialog.value.component)
        assertEquals(40, dependencies.runnerResultsRepository.get(original.id)?.relevanceScore)
        assertEquals(40, pageState.value.view?.page?.savedResults?.single()?.relevanceScore)
        dependencies.runnerResultsRepository.failRelevanceSave = false
        runOnUiThread { dialog.onRun() }
        await(component.dialog) { it.component == null }
        assertEquals(0, dependencies.runnerResultsRepository.get(original.id)?.relevanceScore)
    }

    @Test
    fun closingCancelsTheRequestAndALateResponseCannotReplaceTheOldScore() = runBlocking<Unit> {
        val component = component()
        val dialog = open(component)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        dependencies.relevanceResponse = { _, _ ->
            withContext(NonCancellable) {
                entered.complete(Unit)
                release.await()
                returned.complete(Unit)
                RunnerRelevanceScore(99)
            }
        }
        runOnUiThread { dialog.onRun() }
        withTimeout(5000) { entered.await() }
        assertTrue(component.isBusy)
        runOnUiThread { assertTrue(component.onBack()) }
        assertNull(component.dialog.value.component)
        release.complete(Unit)
        withTimeout(5000) { returned.await() }
        runOnUiThread { assertFalse(component.isBusy) }
        assertEquals(40, dependencies.runnerResultsRepository.get(original.id)?.relevanceScore)
    }

    @Test
    fun disabledProvidersAndDeletedRowsCannotReplaceTheSavedScore() = runBlocking<Unit> {
        val component = component()
        val dialog = open(component)
        dependencies.relevanceResponse = { _, _ ->
            dependencies.providerSettingsUseCases.setActive(
                dialog.state.value.connectionId!!,
                false,
            )
            RunnerRelevanceScore(95)
        }
        runOnUiThread { dialog.onRun() }
        error(dialog)
        assertEquals(40, dependencies.runnerResultsRepository.get(original.id)?.relevanceScore)
        runOnUiThread { component.onBack() }
        dependencies.providerSettingsUseCases.setActive(1, true)
        val retry = open(component)
        dependencies.runnerResultsRepository.records.clear()
        runOnUiThread { retry.onRun() }
        assertTrue(error(retry).error!!.contains("no longer exists"))
        assertEquals(40, pageState.value.view?.page?.savedResults?.single()?.relevanceScore)
    }
}
