package reframecv.ui.components.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class RootNavigationStateTest : ComponentTest() {
    private val owner = ScreenStateOwner(ScreenType.Application)

    @Test
    fun restoresPanelAfterRootDestructionAndPersistsBothWidths() = runBlocking {
        var root = newRoot()
        awaitRestoration(root)
        runOnUiThread {
            assertEquals(true, root.navigationPanel.state.value.expanded)
            root.navigationPanel.onToggle()
            rootLifecycles.last().destroy()
        }
        assertEquals(
            "collapsed",
            dependencies.screenStateUseCases.load(owner)?.text("navigation_panel"),
        )
        root = newRoot()
        awaitRestoration(root)
        runOnUiThread {
            assertEquals(false, root.navigationPanel.state.value.expanded)
            root.navigationPanel.onToggle()
        }
        assertEquals(
            "expanded",
            dependencies.screenStateUseCases.load(owner)?.text("navigation_panel"),
        )
    }

    @Test
    fun invalidPanelStateDefaultsToExpandedAndPreservesOtherApplicationState() = runBlocking {
        dependencies.screenStateUseCases.update(
            owner,
            buildJsonObject {
                put("navigation_panel", true)
                put("other", "unchanged")
            },
        )
        val root = newRoot()
        awaitRestoration(root)
        runOnUiThread {
            assertEquals(true, root.navigationPanel.state.value.expanded)
            root.navigationPanel.onToggle()
        }
        assertEquals("unchanged", dependencies.screenStateUseCases.load(owner)?.text("other"))
    }

    private fun newRoot(): DefaultRootComponent {
        lateinit var root: DefaultRootComponent
        runOnUiThread {
            val lifecycle = LifecycleRegistry().apply { resume() }
            root = DefaultRootComponent(
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), dependencies),
            )
            rootLifecycles += lifecycle
        }
        return root
    }

    private val rootLifecycles = mutableListOf<LifecycleRegistry>()

    @org.junit.After
    fun destroyRoots() = runOnUiThread { rootLifecycles.forEach { it.destroy() } }

    private suspend fun awaitRestoration(root: DefaultRootComponent) {
        val restored = CompletableDeferred<Unit>()
        runOnUiThread {
            root.navigationPanel.state.subscribe { if (!it.loading) restored.complete(Unit) }
        }
        withTimeout(5000) { restored.await() }
    }
}
