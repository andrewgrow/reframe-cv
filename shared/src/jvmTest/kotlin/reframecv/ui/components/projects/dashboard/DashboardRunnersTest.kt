package reframecv.ui.components.projects.dashboard

import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.ComponentTest
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.threading.runOnUiThread

class DashboardRunnersTest : ComponentTest() {
    private suspend fun runner(name: String = "Example search", active: Boolean = true): Long {
        val owner = dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example connection",
                configuration = ApifyConfiguration("token"),
                active = active,
            ),
        )
        return dependencies.connectionRunnersUseCases.save(
            RunnerDraft(
                name = name,
                searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin",
            )
                .applyTo(RunnerDraft.newRunner(owner)),
        )
    }
    private suspend fun dashboard(project: Long = 1, back: () -> Unit = {}): DashboardComponent =
        runOnUiThread {
            lifecycle.resume()
            DefaultDashboardComponent(appComponentContext(), projectId = project, back = back)
        }
    private suspend fun await(
        component: DashboardRunnersComponent,
        check: (DashboardRunnersState) -> Boolean,
    ): DashboardRunnersState {
        val ready = CompletableDeferred<DashboardRunnersState>()
        val subscription =
            runOnUiThread { component.state.subscribe { if (check(it)) ready.complete(it) } }
        return try {
            withTimeout(5000) { ready.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun emptyPickerAndCancelLeaveTheDashboardInPlace() = runBlocking<Unit> {
        var backs = 0
        val dashboard = dashboard { backs++ }
        runOnUiThread { dashboard.runners.open() }
        val picker = await(dashboard.runners) { it.visible && !it.loading }
        assertTrue(picker.runners.isEmpty())
        runOnUiThread { dashboard.onBack() }
        assertFalse(dashboard.runners.state.value.visible)
        assertEquals(0, backs)
        assertEquals(0, dependencies.runnerExecutionRepository.starts)
    }

    @Test
    fun pickerIncludesDisabledConnectionsAndOpeningNeverStartsARun() = runBlocking<Unit> {
        val active = runner()
        val disabled = runner("", false)
        val dashboard = dashboard()
        runOnUiThread { dashboard.runners.open() }
        val picker = await(dashboard.runners) { !it.loading && it.runners.size == 2 }
        assertEquals(listOf(active, disabled), picker.runners.map { it.runner.id })
        assertTrue(picker.runners.first().available)
        assertFalse(picker.runners.last().available)
        runOnUiThread { dashboard.runners.select(disabled) }
        val execution = assertNotNull(dashboard.runners.execution.value.child).instance
        assertFalse(execution.state.value.runner.active)
        runOnUiThread { execution.onStart() }
        assertEquals(0, dependencies.runnerExecutionRepository.starts)
        assertEquals(1, execution.state.value.preferredProjectId)
        runOnUiThread { dashboard.onBack() }
        assertNull(dashboard.runners.execution.value.child)
    }

    @Test
    fun failedPickerCanRetryAndLiveUpdatesRemoveDeletedChoices() = runBlocking<Unit> {
        val runner = runner()
        dependencies.connectionRunnersRepository.failRead = true
        val dashboard = dashboard()
        runOnUiThread { dashboard.runners.open() }
        await(dashboard.runners) { it.failed }
        dependencies.connectionRunnersRepository.failRead = false
        runOnUiThread { dashboard.runners.open() }
        await(dashboard.runners) { !it.loading && !it.failed && it.runners.size == 1 }
        dependencies.connectionRunnersUseCases.delete(runner)
        await(dashboard.runners) { !it.loading && it.runners.isEmpty() }
        runOnUiThread { dashboard.runners.select(runner) }
        assertNull(dashboard.runners.execution.value.child)
    }

    @Test
    fun dashboardBackCannotDismissAPaidStartAndProjectContextIsPassedToResults() =
        runBlocking<Unit> {
            val runner = runner()
            var backs = 0
            val dashboard = dashboard(7) { backs++ }
            runOnUiThread { dashboard.runners.open() }
            await(dashboard.runners) { !it.loading && it.runners.isNotEmpty() }
            runOnUiThread { dashboard.runners.select(runner) }
            val execution = assertNotNull(dashboard.runners.execution.value.child).instance
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            dependencies.runnerExecutionRepository.beforeStart =
                {
                    entered.complete(Unit)
                    release.await()
                }
            runOnUiThread { execution.onStart() }
            withTimeout(5000) { entered.await() }
            runOnUiThread {
                assertTrue(dashboard.isBusy)
                execution.settings.open()
                assertNull(execution.settings.editor.value.child)
                dashboard.onBack()
            }
            assertNotNull(dashboard.runners.execution.value.child)
            assertEquals(0, backs)
            assertEquals(7, execution.state.value.preferredProjectId)
            val ready = CompletableDeferred<Unit>()
            val subscription = runOnUiThread {
                execution.state.subscribe {
                    if (it.view?.page !=
                        null
                    ) {
                        ready.complete(Unit)
                    }
                }
            }
            release.complete(Unit)
            withTimeout(5000) { ready.await() }
            runOnUiThread {
                subscription.cancel()
                assertFalse(dashboard.isBusy)
                dashboard.onBack()
                dashboard.onBack()
            }
            assertNull(dashboard.runners.execution.value.child)
            assertEquals(0, backs)
        }

    @Test
    fun leavingTheDashboardCancelsLocalPollingWithoutStartingAnotherRun() = runBlocking<Unit> {
        val runner = runner()
        val run = dependencies.runnerExecutionUseCases.start(runner)
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        dependencies.runnerExecutionRepository.beforeRead = {
            entered.complete(Unit)
            try {
                CompletableDeferred<Unit>().await()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val dashboard = dashboard()
        runOnUiThread { dashboard.runners.open() }
        await(dashboard.runners) { !it.loading && it.runners.isNotEmpty() }
        runOnUiThread { dashboard.runners.select(runner) }
        val execution = assertNotNull(dashboard.runners.execution.value.child).instance
        val loaded = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            execution.state.subscribe { if (!it.loadingHistory) loaded.complete(Unit) }
        }
        withTimeout(5000) { loaded.await() }
        runOnUiThread {
            subscription.cancel()
            execution.onSelect(run.id)
        }
        withTimeout(5000) { entered.await() }
        runOnUiThread { lifecycle.stop() }
        withTimeout(5000) { cancelled.await() }
        assertFalse(execution.state.value.reading)
        assertEquals(1, dependencies.runnerExecutionRepository.starts)
    }

    @Test
    fun runnerSettingsOpenTheOwningConnectionAndReturnToTheSameResults() = runBlocking<Unit> {
        runner("Other search")
        val id = runner("Target search")
        val dashboard = dashboard()
        runOnUiThread { dashboard.runners.open() }
        val picker = await(dashboard.runners) { !it.loading && it.runners.size == 2 }
        val ownerId = picker.runners.last().runner.connectionId
        runOnUiThread { dashboard.runners.select(id) }
        val execution = assertNotNull(dashboard.runners.execution.value.child).instance
        runOnUiThread { execution.settings.open() }
        val opened = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            execution.state.subscribe { if (!it.openingSettings) opened.complete(Unit) }
        }
        withTimeout(5000) { opened.await() }
        runOnUiThread { subscription.cancel() }
        val editor = assertNotNull(execution.settings.editor.value.child).instance
        assertEquals(ownerId, editor.state.value.connection.id)
        assertNotNull(editor.runners.value.child)
        dependencies.providerSettingsUseCases.setActive(ownerId, false)
        runOnUiThread {
            dashboard.onBack()
            assertNull(execution.settings.editor.value.child)
            assertEquals(execution, dashboard.runners.execution.value.child?.instance)
        }
        val refreshed = CompletableDeferred<Unit>()
        val refreshSubscription = runOnUiThread {
            execution.state.subscribe { if (!it.openingSettings) refreshed.complete(Unit) }
        }
        withTimeout(5000) { refreshed.await() }
        runOnUiThread { refreshSubscription.cancel() }
        assertFalse(execution.state.value.runner.active)
        assertEquals(0, dependencies.runnerExecutionRepository.starts)
    }
}
