package reframecv.ui.components.settings.runners

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.ProviderConnection
import reframecv.runners.RunnerExecutionError
import reframecv.runners.RunnerExecutionException
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class RunnerExecutionTest : ComponentTest() {
    private suspend fun runner(): ConnectionRunner {
        val owner = dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example source",
                configuration = ApifyConfiguration("fictional-token"),
            ),
        )
        val id = dependencies.connectionRunnersUseCases.save(
            RunnerDraft(searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin")
                .applyTo(RunnerDraft.newRunner(owner)),
        )
        return assertNotNull(dependencies.connectionRunnersRepository.get(id))
    }
    private suspend fun await(
        component: RunnerRunsComponent,
        test: (RunnerRunsState) -> Boolean,
    ): RunnerRunsState {
        val ready = CompletableDeferred<RunnerRunsState>()
        val subscription = runOnUiThread {
            component.state.subscribe { if (test(it)) ready.complete(it) }
        }
        return try {
            withTimeout(5000) { ready.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }
    private suspend fun component(
        runner: ConnectionRunner,
        back: () -> Unit = {},
        poll: suspend () -> Unit = { error("Unexpected polling") },
    ) = runOnUiThread {
        lifecycle.resume()
        DefaultRunnerRunsComponent(appComponentContext(), runner, back, poll)
    }

    @Test
    fun startPersistsIdentityAndSnapshotAndReadsCanResumeWithoutAnotherPost() = runBlocking<Unit> {
        val runner = runner()
        val useCases = dependencies.runnerExecutionUseCases
        val run = useCases.start(runner.id)
        assertEquals("remoteRun1", run.externalId)
        assertEquals(runner.configuration, run.configurationSnapshot)
        assertEquals(ConnectionRunState.Succeeded, run.state)
        assertEquals(run, dependencies.connectionRunnersRepository.getRun(run.id))
        val view = useCases.inspect(runner.id, run.id, 50)
        assertEquals(0, view.page?.offset)
        assertEquals(0, dependencies.runnerExecutionRepository.requestedOffset)
        assertEquals(1, dependencies.runnerExecutionRepository.starts)
        assertEquals(1, dependencies.runnerExecutionRepository.resultReads)
        assertFailsWith<IllegalArgumentException> { useCases.inspect(runner.id + 1, run.id) }
    }

    @Test
    fun disabledConnectionOrRunnerBlocksPaidStartAndDisabledConnectionBlocksReads() =
        runBlocking<Unit> {
            val runner = runner()
            val useCases = dependencies.runnerExecutionUseCases
            val run = useCases.start(runner.id)
            val connection =
                assertNotNull(
                    dependencies.providerSettingsUseCases.observe().first().find {
                        it.id ==
                            runner.connectionId
                    },
                )
            dependencies.providerSettingsUseCases.save(connection.copy(active = false))
            assertFailsWith<RunnerExecutionException> { useCases.start(runner.id) }
            assertFailsWith<RunnerExecutionException> { useCases.inspect(runner.id, run.id) }
            dependencies.providerSettingsUseCases.save(connection)
            dependencies.connectionRunnersUseCases.save(runner.copy(active = false))
            assertFailsWith<RunnerExecutionException> { useCases.start(runner.id) }
            assertEquals(1, dependencies.runnerExecutionRepository.starts)
            assertEquals(0, dependencies.runnerExecutionRepository.reads)
        }

    @Test
    fun ambiguousStartIsRecordedAndOpeningItsHistoryNeverRetriesPost() = runBlocking<Unit> {
        val runner = runner()
        dependencies.runnerExecutionRepository.startError = RunnerExecutionException(
            RunnerExecutionError.Unavailable,
            "Could not contact Apify.",
        )
        val component = component(runner)
        runOnUiThread { component.onStart() }
        await(component) { !it.starting && it.error != null }
        val failed = dependencies.connectionRunnersRepository.history.value.single()
        assertEquals(ConnectionRunState.Failed, failed.state)
        assertNull(failed.externalId)
        assertTrue(failed.error!!.contains("Apify Console"))
        runOnUiThread { component.onSelect(failed.id) }
        await(component) { it.view != null && !it.reading }
        assertEquals(1, dependencies.runnerExecutionRepository.starts)
        assertEquals(0, dependencies.runnerExecutionRepository.reads)
    }

    @Test
    fun explicitStartBlocksDuplicateClicksAndBackThenPollsUntilResults() = runBlocking<Unit> {
        val runner = runner()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val remote = dependencies.runnerExecutionRepository
        remote.beforeStart = {
            entered.complete(Unit)
            release.await()
        }
        remote.status = remote.status.copy(state = ConnectionRunState.Running, finishedAt = null)
        var backs = 0
        val component = component(runner, { backs++ }) {
            remote.status =
                remote.status.copy(state = ConnectionRunState.Succeeded, finishedAt = 999)
        }
        runOnUiThread { component.onStart() }
        withTimeout(5000) { entered.await() }
        runOnUiThread {
            component.onStart()
            component.onBack()
            assertTrue(component.state.value.starting)
        }
        release.complete(Unit)
        await(component) { it.view?.page != null }
        assertEquals(1, remote.starts)
        assertEquals(2, remote.reads)
        assertEquals(0, backs)
        runOnUiThread {
            component.onBack()
            component.onBack()
        }
        assertEquals(1, backs)
    }

    @Test
    fun leavingCancelsLocalReadAndReopeningResumesTheSavedRun() = runBlocking<Unit> {
        val runner = runner()
        val remote = dependencies.runnerExecutionRepository
        val run = dependencies.runnerExecutionUseCases.start(runner.id)
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        remote.beforeRead = {
            entered.complete(Unit)
            try {
                CompletableDeferred<Unit>().await()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val component = component(runner)
        await(component) { !it.loadingHistory }
        runOnUiThread { component.onSelect(run.id) }
        withTimeout(5000) { entered.await() }
        runOnUiThread { component.onBack() }
        withTimeout(5000) { cancelled.await() }
        assertNull(component.state.value.view)
        remote.beforeRead = {}
        runOnUiThread { component.onSelect(run.id) }
        await(component) { it.view?.page != null }
        assertEquals(1, remote.starts)
    }

    @Test
    fun readFailurePreservesSelectionAndRefreshRetriesOnlyGetsAndSupportsPaging() =
        runBlocking<Unit> {
            val runner = runner()
            val remote = dependencies.runnerExecutionRepository
            remote.readError =
                RunnerExecutionException(RunnerExecutionError.Unauthorized, "Token expired.")
            val component = component(runner)
            runOnUiThread { component.onStart() }
            val failed = await(component) { it.error != null }
            assertEquals("Token expired.", failed.error)
            assertNotNull(failed.view)
            remote.readError = null
            runOnUiThread { component.onRefresh() }
            await(component) { it.view?.page != null }
            runOnUiThread { component.onPage(50) }
            await(component) { !it.reading && it.view?.page?.offset == 0 }
            assertEquals(1, remote.starts)
            assertFalse(component.state.value.reading)
        }

    @Test
    fun failedAndAbortedRunsKeepDetailsAndMissingDatasetCanBeRetried() = runBlocking<Unit> {
        val runner = runner()
        val useCases = dependencies.runnerExecutionUseCases
        val remote = dependencies.runnerExecutionRepository
        val run = useCases.start(runner.id)
        listOf(ConnectionRunState.Failed, ConnectionRunState.Aborted).forEach { status ->
            remote.status = remote.status.copy(state = status, details = "Example failure")
            val view = useCases.inspect(runner.id, run.id)
            assertNull(view.page)
            assertEquals("Example failure", view.run.error)
        }
        assertEquals(0, remote.resultReads)
        remote.status = remote.status.copy(state = ConnectionRunState.Succeeded, datasetId = null)
        val stored = dependencies.connectionRunnersRepository.getRun(run.id)!!
        dependencies.connectionRunnersRepository.updateRun(stored.copy(datasetId = null))
        assertFailsWith<RunnerExecutionException> { useCases.inspect(runner.id, run.id) }
        remote.status = remote.status.copy(datasetId = "dataset1")
        assertNotNull(useCases.inspect(runner.id, run.id).page)
        assertNull(dependencies.connectionRunnersRepository.getRun(run.id)?.error)
        val broken = runner.copy(configuration = "not JSON")
        dependencies.connectionRunnersRepository.save(broken)
        assertFailsWith<RunnerExecutionException> { useCases.start(runner.id) }
        assertEquals(1, remote.starts)
    }

    @Test
    fun cancellationDuringStartPreservesAmbiguousLocalHistory() = runBlocking<Unit> {
        val runner = runner()
        val entered = CompletableDeferred<Unit>()
        dependencies.runnerExecutionRepository.beforeStart = {
            entered.complete(Unit)
            CompletableDeferred<Unit>().await()
        }
        val work = launch { dependencies.runnerExecutionUseCases.start(runner.id) }
        withTimeout(5000) { entered.await() }
        work.cancelAndJoin()
        val run = dependencies.connectionRunnersRepository.history.value.single()
        assertEquals(ConnectionRunState.Failed, run.state)
        assertNull(run.externalId)
        assertTrue(run.error!!.contains("Apify Console"))
    }
}
