package reframecv.ui.components.application

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.testing.ComponentTest
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.application.navigation.visibleRows
import reframecv.ui.threading.runOnUiThread

class RootProjectTreeTest : ComponentTest() {
    @Test
    fun navigatesBetweenBranchesObservesNamesAndReturnsToSurvivingParent() = runBlocking {
        val repository = dependencies.projectsRepository
        val backend = repository.createProject("Backend")
        val java = repository.createProject("Java", backend.id)
        val mobile = repository.createProject("Mobile")
        val android = repository.createProject("Android", mobile.id)
        lateinit var root: DefaultRootComponent
        val updates = Channel<ProjectTreeState>(Channel.UNLIMITED)
        runOnUiThread {
            root = DefaultRootComponent(appComponentContext())
            lifecycle.resume()
            root.projectTree.state.subscribe { updates.trySend(it) }
        }
        withTimeout(10_000L) {
            awaitState(updates) { !it.loading }
            runOnUiThread { root.projectTree.onProjectSelected(java.id) }
            val javaState = awaitState(updates) { it.selectedId == java.id }
            assertTrue(backend.id in javaState.expandedIds)
            assertEquals(listOf(backend.id, java.id), activePath(root))
            runOnUiThread { root.projectTree.onProjectSelected(android.id) }
            val androidState = awaitState(updates) { it.selectedId == android.id }
            assertTrue(mobile.id in androidState.expandedIds)
            assertTrue(backend.id in androidState.expandedIds)
            assertEquals(listOf(mobile.id, android.id), activePath(root))
            repository.updateProject(android.id, "Android Developer")
            awaitState(updates) { it.projects.any { p -> p.name == "Android Developer" } }
            repository.deleteProject(android.id)
            val afterDelete = awaitState(updates) { it.selectedId == mobile.id }
            assertTrue(afterDelete.projects.none { it.id == android.id })
            assertEquals(listOf(mobile.id), activePath(root))
            runOnUiThread {
                assertTrue(
                    root.childStack.value.items.none { item ->
                        val component = (item.instance as RootComponent.Child.Projects).component
                        val path = component.projectPath
                        path.any { it.id == android.id }
                    },
                )
            }
            repository.deleteProject(mobile.id)
            awaitState(updates) {
                it.selectedId == null &&
                    it.projects.none { p -> p.id == mobile.id }
            }
            assertEquals(emptyList(), activePath(root))
            runOnUiThread {
                root.projectTree.onProjectSelected(java.id)
                root.projectTree.onToggle(backend.id)
                assertTrue(
                    root.projectTree.state.value.visibleRows().none {
                        it.project.id ==
                            java.id
                    },
                )
                root.onProjectsList()
                assertEquals(null, root.projectTree.state.value.selectedId)
            }
        }
    }

    private suspend fun awaitState(
        updates: Channel<ProjectTreeState>,
        predicate: (ProjectTreeState) -> Boolean,
    ): ProjectTreeState {
        while (true) {
            val state = updates.receive()
            if (predicate(state)) return state
        }
    }

    private fun activePath(root: RootComponent): List<Long> = runOnUiThread {
        val stack: ChildStack<*, RootComponent.Child> = root.childStack.value
        (stack.active.instance as RootComponent.Child.Projects).component.projectPath.map { it.id }
    }
}
