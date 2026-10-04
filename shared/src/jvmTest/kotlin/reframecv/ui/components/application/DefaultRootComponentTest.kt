package reframecv.ui.components.application

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import reframecv.domain.models.project.Project
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.UiState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.threading.runOnUiThread

class DefaultRootComponentTest : ComponentTest() {
    @Test
    fun opensNestedProjectsAndReturnsThroughBreadcrumbsAndMenu() = runOnUiThread {
        val root = DefaultRootComponent(appComponentContext())
        lifecycle.resume()
        val rootProjects =
            assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance).component
        val parent = Project(id = 1, name = "Backend", createdAt = 0, updatedAt = 0)
        val child =
            Project(id = 2, name = "Google", createdAt = 0, updatedAt = 0, parentId = parent.id)
        rootProjects.onOpenProject(parent)
        val parentProjects =
            assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance).component
        assertEquals(listOf(ProjectBreadcrumb(parent.id, parent.name)), parentProjects.projectPath)
        assertEquals(UiState.NoProjects, parentProjects.uiState.value)
        parentProjects.onOpenProject(child)
        val childProjects =
            assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance).component
        assertEquals(
            parentProjects.projectPath + ProjectBreadcrumb(child.id, child.name),
            childProjects.projectPath,
        )
        assertEquals(2, root.childStack.value.backStack.size)
        childProjects.onBreadcrumb(1)
        assertSame(
            parentProjects,
            assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance).component,
        )
        root.onProjectsList()
        assertSame(
            rootProjects,
            assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance).component,
        )
        assertTrue(root.childStack.value.backStack.isEmpty())
    }

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
