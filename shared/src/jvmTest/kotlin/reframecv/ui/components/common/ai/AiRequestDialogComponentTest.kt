package reframecv.ui.components.common.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class AiRequestDialogComponentTest : ComponentTest() {
    @Test
    fun finishedRequestsRefreshBalanceWithoutDelayingResultsOrHidingRequestErrors() = runBlocking {
        dependencies.providerSettingsUseCases.save(
            connection("Work").copy(managementApiKey = "management"),
        )
        dependencies.promptsUseCases.save(Prompt(text = "Assess"))
        val balanceStarted = CompletableDeferred<Unit>()
        val balanceResponse =
            CompletableDeferred<reframecv.domain.models.providers.ProviderCreditBalance>()
        dependencies.balanceResponse = {
            balanceStarted.complete(Unit)
            balanceResponse.await()
        }
        val completed = CompletableDeferred<Unit>()
        val success = runOnUiThread {
            DefaultAiRequestDialogComponent(
                appComponentContext(),
                "resume",
                PromptComponent.Vacancies,
                execute = { _, _ ->
                    "result"
                },
                completed = { completed.complete(Unit) },
                dismissed = {},
            )
        }
        await(success) { !it.loading }
        runOnUiThread { success.onRun() }
        withTimeout(5000) {
            completed.await()
            balanceStarted.await()
        }
        assertFalse(balanceResponse.isCompleted)
        balanceResponse.complete(
            reframecv.domain.models.providers.ProviderCreditBalance(100.0, 0.01, 1),
        )
        val shown = CompletableDeferred<Unit>()
        runOnUiThread {
            dependencies.aiRequestBalance.state.subscribe {
                if (it.balance !=
                    null
                ) {
                    shown.complete(Unit)
                }
            }
        }
        withTimeout(5000) { shown.await() }
        assertEquals(listOf("management"), dependencies.balanceRequests)
        val refreshed = CompletableDeferred<Unit>()
        dependencies.balanceResponse = {
            refreshed.complete(Unit)
            reframecv.domain.models.providers.ProviderCreditBalance(100.0, 0.02, 2)
        }
        val failure = create()
        await(failure) { !it.loading }
        runOnUiThread { failure.onRun() }
        await(failure) { it.error == "Rejected" && !it.running }
        withTimeout(5000) { refreshed.await() }
        assertTrue(failure.state.value.canRun)
    }

    @Test
    fun filtersDisabledAndOtherComponentChoicesAndRequiresExplicitSelectionWhenAmbiguous() =
        runBlocking {
            val first = dependencies.providerSettingsUseCases.save(connection("First"))
            dependencies.providerSettingsUseCases.save(connection("Disabled").copy(active = false))
            dependencies.providerSettingsUseCases.save(connection("Second"))
            dependencies.promptsUseCases.save(Prompt(text = "General"))
            dependencies.promptsUseCases.save(
                Prompt(text = "Vacancies", component = PromptComponent.Vacancies),
            )
            dependencies.promptsUseCases.save(
                Prompt(text = "Resume", component = PromptComponent.Resume),
            )
            dependencies.promptsUseCases.save(Prompt(text = "Disabled", active = false))
            val dialog = create()
            await(dialog) { !it.loading }
            runOnUiThread {
                assertEquals(
                    listOf("First", "Second"),
                    dialog.state.value.connections.map {
                        it.name
                    },
                )
                assertEquals(
                    listOf("General", "Vacancies"),
                    dialog.state.value.prompts.map {
                        it.text
                    },
                )
                assertFalse(dialog.state.value.canRun)
                dialog.onConnection(first)
                dialog.onPrompt(1)
                assertTrue(dialog.state.value.canRun)
                dialog.onPrompt(3)
                assertEquals(1L, dialog.state.value.promptId)
            }
            dependencies.providerSettingsUseCases.setActive(first, false)
            await(dialog) { it.connectionId != first }
            runOnUiThread { assertEquals("Second", dialog.state.value.connection?.name) }
        }

    @Test
    fun editsAreRequestOnlyUntilSavePromptAndSavingPreservesOtherPromptFields() = runBlocking {
        dependencies.providerSettingsUseCases.save(connection(""))
        dependencies.promptsUseCases.save(Prompt(title = "Original", text = "Original text"))
        var sent = ""
        val result = CompletableDeferred<String>()
        val dialog = runOnUiThread {
            DefaultAiRequestDialogComponent(
                appComponentContext(),
                "job.pdf",
                PromptComponent.Vacancies,
                execute = { _, text ->
                    sent = text
                    "result"
                },
                completed = { result.complete(it) },
                dismissed = {},
            )
        }
        await(dialog) { !it.loading }
        runOnUiThread { dialog.onText("New draft") }
        assertEquals("Original text", dependencies.promptsRepository.records.value.single().text)
        runOnUiThread { dialog.onSavePrompt() }
        await(dialog) { it.promptSaved && !it.savingPrompt }
        assertEquals("New draft", dependencies.promptsRepository.records.value.single().text)
        assertEquals("Original", dependencies.promptsRepository.records.value.single().title)
        runOnUiThread {
            dialog.onText("One-off change")
            dialog.onRun()
        }
        assertEquals("result", withTimeout(10_000) { result.await() })
        assertEquals("One-off change", sent)
        assertEquals("New draft", dependencies.promptsRepository.records.value.single().text)
    }

    @Test
    fun saveFailureAndRequestFailureKeepEditableDraftForRetry() = runBlocking {
        dependencies.providerSettingsUseCases.save(connection("Test"))
        dependencies.promptsUseCases.save(Prompt(text = "Original"))
        dependencies.promptsRepository.failSave = true
        val dialog = create()
        await(dialog) { !it.loading }
        runOnUiThread {
            dialog.onText("Edited")
            dialog.onSavePrompt()
        }
        await(dialog) { it.error != null && !it.savingPrompt }
        assertEquals("Original", dependencies.promptsRepository.records.value.single().text)
        runOnUiThread {
            assertEquals("Edited", dialog.state.value.text)
            dialog.onRun()
        }
        await(dialog) { it.error == "Rejected" && !it.running }
        assertTrue(dialog.state.value.canRun)
        runOnUiThread { dialog.onText("Retry text") }
        assertEquals("Retry text", dialog.state.value.text)
    }

    @Test
    fun emptyChoicesAndIncompleteConnectionCannotRunAndCancelNeverCompletes() = runBlocking {
        var closes = 0
        var calls = 0
        val dialog = runOnUiThread {
            DefaultAiRequestDialogComponent(
                appComponentContext(),
                "job.pdf",
                PromptComponent.Vacancies,
                execute = { _, _ ->
                    calls++
                    "result"
                },
                completed = { error("No result expected") },
                dismissed = { closes++ },
            )
        }
        await(dialog) { !it.loading }
        runOnUiThread {
            dialog.onRun()
            assertFalse(dialog.state.value.canRun)
        }
        dependencies.providerSettingsUseCases.save(connection("Incomplete").copy(modelId = ""))
        dependencies.promptsUseCases.save(Prompt(text = "Extract"))
        await(dialog) { it.connection != null && it.prompt != null }
        runOnUiThread {
            assertFalse(dialog.state.value.canRun)
            dialog.onRun()
            dialog.onClose()
            dialog.onClose()
        }
        assertEquals(0, calls)
        assertEquals(1, closes)
    }

    private fun create() = runOnUiThread {
        DefaultAiRequestDialogComponent<String>(
            appComponentContext(),
            "job.pdf",
            PromptComponent.Vacancies,
            execute = { _, _ ->
                throw AiRequestException("Rejected")
            },
            completed = {},
            dismissed = {},
        )
    }

    private suspend fun await(
        dialog: AiRequestDialogComponent,
        predicate: (AiRequestDialogState) -> Boolean,
    ) {
        val ready = CompletableDeferred<Unit>()
        runOnUiThread { dialog.state.subscribe { if (predicate(it)) ready.complete(Unit) } }
        withTimeout(10_000) { ready.await() }
    }

    private fun connection(name: String) = ProviderConnection(
        type = ApiProviderType.OpenRouter,
        name = name,
        apiKey = "test-key",
        modelId = "test-model",
    )
}
