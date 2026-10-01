package ankideckbuilder.ui.components.application

import ankideckbuilder.testing.ComponentTest
import ankideckbuilder.ui.components.projects.UiState
import ankideckbuilder.ui.threading.runOnUiThread
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
