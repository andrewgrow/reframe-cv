package reframecv.ui.components.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.UiState
import reframecv.ui.threading.runOnUiThread

class DefaultRootComponentTest : ComponentTest() {
    @Test
    fun startsWithProjectsAsTheOnlyScreen() {
        runOnUiThread {
            val root = DefaultRootComponent(DefaultComponentContext(lifecycle))
            lifecycle.resume()
            val stack = root.childStack.value
            assertTrue(stack.backStack.isEmpty())
            val child = assertIs<RootComponent.Child.Projects>(stack.active.instance)
            assertEquals(UiState.NoProjects, child.component.uiState.value)
        }
    }
}
