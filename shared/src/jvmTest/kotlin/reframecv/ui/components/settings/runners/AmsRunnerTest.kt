package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.AmsJobSearchConfiguration
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class AmsRunnerTest : ComponentTest() {
    @Test
    fun validatesAmsSearchAndRoundTripsSettingsWithoutChangingLinkedInDefaults() {
        val draft = RunnerDraft(
            operation = ConnectionOperation.AmsJobSearch,
            query = " Kotlin ",
            location = " Wien ",
            maxResults = "25",
            postedWithin = "LAST_MONTH",
        )
        assertTrue(draft.valid)
        val runner = draft.applyTo(RunnerDraft.newRunner(1))
        val config = assertIs<AmsJobSearchConfiguration>(RunnerConfigurationJson.decode(runner))
        assertEquals("Kotlin", config.query)
        assertEquals("Wien", config.location)
        assertTrue(config.includeExternalJobs)
        assertEquals(draft.copy(query = "Kotlin", location = "Wien"), RunnerDraft.from(runner))
        assertTrue(draft.copy(location = "", radiusKm = "0").valid)
        listOf(
            draft.copy(query = " "),
            draft.copy(radiusKm = "-1"),
            draft.copy(radiusKm = "invalid"),
            draft.copy(location = ""),
            draft.copy(maxResults = "10001"),
            draft.copy(postedWithin = "LAST_DAY"),
        )
            .forEach { assertFalse(it.valid) }
        assertFalse(config.copy(actorId = "other/actor").valid)
        assertFalse(config.copy(version = 2).valid)
        assertFalse(config.copy(query = "").valid)
        assertTrue(RunnerDraft(searchUrls = "https://www.linkedin.com/jobs/search").valid)
    }

    @Test
    fun newEditorCanSaveAmsAndExistingRunnerCannotChangeOperation() = runBlocking {
        val owner = dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                name = "Example",
                type = ApiProviderType.Apify,
                configuration = ApifyConfiguration("token"),
            ),
        )
        val editor = runOnUiThread {
            lifecycle.resume()
            val editor = DefaultRunnerEditorComponent(
                appComponentContext(),
                RunnerDraft.newRunner(owner),
            ) {}
            editor.onDraft(
                RunnerDraft(
                    operation = ConnectionOperation.AmsJobSearch,
                    query = "Android",
                    name = "AMS Vienna",
                    maxResults = "25",
                ),
            )
            editor.onSave()
            editor
        }
        await(editor.state) { it.successfulSaves == 1L }
        val copy = runOnUiThread {
            assertEquals(ConnectionOperation.AmsJobSearch, editor.state.value.runner.operation)
            editor.onDraft(
                editor.state.value.draft.copy(operation = ConnectionOperation.LinkedInJobSearch),
            )
            assertEquals(ConnectionOperation.AmsJobSearch, editor.state.value.draft.operation)
            val copy = DefaultRunnerEditorComponent(
                appComponentContext(),
                RunnerDraft.newRunner(owner),
            ) {}
            copy.onDraft(RunnerDraft(operation = ConnectionOperation.AmsJobSearch))
            copy.copy.open()
            copy
        }
        await(copy.copy.state) { !it.loading && it.sources.isNotEmpty() }
        runOnUiThread {
            assertEquals(1, copy.copy.state.value.sources.size)
            copy.copy.select(editor.state.value.runner.id)
            copy.copy.apply()
            assertEquals("Android", copy.state.value.draft.query)
        }
    }

    @Test
    fun executionPassesAmsSettingsAndOperationToTheRemoteAdapter() = runBlocking {
        val owner = dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                name = "Example",
                type = ApiProviderType.Apify,
                configuration = ApifyConfiguration("token"),
            ),
        )
        val runner = RunnerDraft(operation = ConnectionOperation.AmsJobSearch, query = "Kotlin")
            .applyTo(RunnerDraft.newRunner(owner))
        val id = dependencies.connectionRunnersUseCases.save(runner)
        val run = dependencies.runnerExecutionUseCases.start(id)
        assertIs<AmsJobSearchConfiguration>(
            dependencies.runnerExecutionRepository.startedConfiguration,
        )
        dependencies.runnerExecutionUseCases.inspect(id, run.id)
        assertEquals(
            ConnectionOperation.AmsJobSearch,
            dependencies.runnerExecutionRepository.requestedOperation,
        )
        assertEquals(1, dependencies.runnerExecutionRepository.starts)
    }

    private suspend fun <T : Any> await(value: Value<T>, condition: (T) -> Boolean): T {
        val result = CompletableDeferred<T>()
        val subscription = runOnUiThread {
            value.subscribe { if (condition(it)) result.complete(it) }
        }
        return try {
            withTimeout(5000) { result.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }
}
