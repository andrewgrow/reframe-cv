package reframecv.ui.components.application

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.UiState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.threading.runOnUiThread

class DefaultRootComponentTest : ComponentTest() {
    @Test
    fun startsWithProjectsAsTheOnlyScreen() {
        runOnUiThread {
            val root =
                DefaultRootComponent(appComponentContext())
            lifecycle.resume()
            val stack = root.childStack.value
            assertTrue(stack.backStack.isEmpty())
            val child = assertIs<RootComponent.Child.Projects>(stack.active.instance)
            assertEquals(UiState.NoProjects, child.component.uiState.value)
            val childContext = child.component as AppComponentContext
            assertSame(dependencies, childContext.dependencies)
            assertNotSame(lifecycle, childContext.lifecycle)
            child.component.onAddProject()
            val editor = child.component.editorSlot.value.child?.instance
            val editorContext = editor as AppComponentContext
            assertSame(dependencies, editorContext.dependencies)
            assertNotSame(childContext.lifecycle, editorContext.lifecycle)
            repeat(2) { root.onProjectsList() }
            assertSame(child, root.childStack.value.active.instance)
            assertTrue(root.childStack.value.backStack.isEmpty())
            assertSame(editor, child.component.editorSlot.value.child?.instance)
        }
    }
}
