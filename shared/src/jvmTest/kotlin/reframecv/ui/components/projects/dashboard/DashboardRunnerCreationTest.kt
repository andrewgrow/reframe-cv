package reframecv.ui.components.projects.dashboard

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class DashboardRunnerCreationTest : ComponentTest() {
    private suspend fun dashboard() = runOnUiThread {
        lifecycle.resume()
        DefaultDashboardComponent(appComponentContext(), projectId = 1)
    }
    private suspend fun await(component: DashboardRunnerCreationComponent) {
        val ready = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            component.state.subscribe { if (!it.loading) ready.complete(Unit) }
        }
        try {
            withTimeout(5000) { ready.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun creationUsesOnlyApifyConnectionsAndSavesWithoutStartingARun() = runBlocking<Unit> {
        dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "AI connection",
                configuration = OpenRouterConfiguration(apiKey = "fictional-token"),
            ),
        )
        val owner = dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example connection",
                configuration = ApifyConfiguration("fictional-token"),
            ),
        )
        val dashboard = dashboard()
        runOnUiThread {
            dashboard.runners.open()
            dashboard.runners.onAdd()
        }
        val creation = dashboard.runners.creation
        await(creation)
        assertFalse(dashboard.runners.state.value.visible)
        assertEquals(listOf(owner), creation.state.value.connections.map { it.id })
        runOnUiThread { creation.select(owner) }
        val editor = assertNotNull(creation.editor.value.child).instance
        assertEquals(owner, editor.state.value.runner.connectionId)
        assertEquals(0, editor.state.value.runner.id)
        val saved = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            editor.state.subscribe { if (it.successfulSaves > 0) saved.complete(Unit) }
        }
        runOnUiThread {
            editor.onDraft(
                editor.state.value.draft.copy(
                    name = "Example search",
                    searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin",
                ),
            )
            editor.onSave()
        }
        withTimeout(5000) { saved.await() }
        runOnUiThread {
            subscription.cancel()
            assertTrue(editor.state.value.runner.id > 0)
            dashboard.onBack()
        }
        assertNull(creation.editor.value.child)
        assertEquals(
            "Example search",
            dependencies.connectionRunnersUseCases.observe(owner).first().single().name,
        )
        assertEquals(0, dependencies.runnerExecutionRepository.starts)
    }

    @Test
    fun emptySelectionAndCancelledDraftDoNotCreateRecordsOrLeaveTheDashboard() = runBlocking<Unit> {
        val dashboard = dashboard()
        val creation = dashboard.runners.creation
        runOnUiThread {
            dashboard.runners.open()
            dashboard.runners.onAdd()
        }
        await(creation)
        assertTrue(creation.state.value.connections.isEmpty())
        runOnUiThread {
            creation.select(999)
            assertNull(creation.editor.value.child)
            dashboard.onBack()
            assertFalse(creation.state.value.visible)
        }
        val owner = dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Disabled connection",
                configuration = ApifyConfiguration("fictional-token"),
                active = false,
            ),
        )
        runOnUiThread { creation.open() }
        await(creation)
        runOnUiThread {
            creation.select(owner)
            assertNotNull(creation.editor.value.child)
            dashboard.onBack()
        }
        assertNull(creation.editor.value.child)
        assertTrue(dependencies.connectionRunnersUseCases.observe(owner).first().isEmpty())
        assertEquals(0, dependencies.runnerExecutionRepository.starts)
    }
}
