package reframecv.ui.components.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.dependencies.DefaultApplicationDependencies
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class RootWorkspaceNavigationTest : WorkspaceDatabaseTest() {
    @Test
    fun treeOpensWorkspaceDashboardAndKeepsContainerAndUnconfiguredScreens() = runBlocking<Unit> {
        val dependencies = DefaultApplicationDependencies { database }
        val repository = dependencies.projectsRepository
        val workspace = repository.createProject("Workspace")
        dependencies.resumesRepository.create(resume(workspace.id))
        val parent = repository.createProject("Container")
        val empty = repository.createProject("Empty", parent.id)
        val lifecycle = LifecycleRegistry()
        val states = Channel<ProjectTreeState>(Channel.UNLIMITED)
        lateinit var root: DefaultRootComponent
        try {
            runOnUiThread {
                lifecycle.resume()
                root =
                    DefaultRootComponent(
                        DefaultAppComponentContext(
                            DefaultComponentContext(lifecycle),
                            dependencies,
                        ),
                    )
                root.projectTree.state.subscribe { states.trySend(it) }
            }
            withTimeout(10_000.milliseconds) {
                while (states.receive().loading) { /* Wait for the tree's initial read. */ }
            }
            runOnUiThread {
                root.projectTree.onProjectSelected(workspace.id)
                val dashboard =
                    assertIs<RootComponent.Child.Dashboard>(root.childStack.value.active.instance)
                assertEquals(workspace.id, dashboard.component.projectId)
                root.projectTree.onProjectSelected(parent.id)
                val container =
                    assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
                assertEquals(parent.id, container.component.projectPath.last().id)
                root.projectTree.onProjectSelected(empty.id)
                val unconfigured =
                    assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
                assertEquals(empty.id, unconfigured.component.projectPath.last().id)
                root.projectTree.onProjectSelected(workspace.id)
                assertIs<RootComponent.Child.Dashboard>(root.childStack.value.active.instance)
            }
        } finally {
            runOnUiThread { lifecycle.destroy() }
            states.close()
        }
    }
}
