package reframecv.ui.components.application

import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.project.ProjectMode
import reframecv.testing.ComponentTest
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.threading.runOnUiThread

class RootDashboardTest : ComponentTest() {
    @Test
    fun configureKeepsSelectionAndBackReturnsWithoutChangingMode() = runBlocking<Unit> {
        val project = dependencies.projectsRepository.createProject("Backend")
        lateinit var root: DefaultRootComponent
        runOnUiThread {
            root = DefaultRootComponent(appComponentContext())
            lifecycle.resume()
            val projects =
                assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
            projects.component.onOpenProject(project)
            val opened =
                assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
            opened.component.onConfigureProject()
            val dashboard =
                assertIs<RootComponent.Child.Dashboard>(root.childStack.value.active.instance)
            assertEquals(project.id, dashboard.component.projectId)
            assertEquals(project.id, root.projectTree.state.value.selectedId)
            (root.backHandler as BackDispatcher).back()
            val returned =
                assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
            assertEquals(project.id, returned.component.projectPath.last().id)
            assertEquals(project.id, root.projectTree.state.value.selectedId)
            returned.component.onConfigureProject()
            root.onProjectsList()
            val rootList =
                assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
            assertEquals(emptyList(), rootList.component.projectPath)
            assertEquals(null, root.projectTree.state.value.selectedId)
        }
        assertEquals(
            ProjectMode.Unconfigured,
            dependencies.projectsRepository.observeProjects().first().single().mode,
        )
    }

    @Test
    fun deletingTheDashboardProjectReturnsToTheRootAndRemovesItsHistory() = runBlocking<Unit> {
        val project = dependencies.projectsRepository.createProject("Backend")
        lateinit var root: DefaultRootComponent
        val updates = Channel<ProjectTreeState>(Channel.UNLIMITED)
        runOnUiThread {
            root = DefaultRootComponent(appComponentContext())
            lifecycle.resume()
            root.projectTree.state.subscribe { updates.trySend(it) }
            assertIs<RootComponent.Child.Projects>(
                root.childStack.value.active.instance,
            ).component.onOpenProject(project)
            assertIs<RootComponent.Child.Projects>(
                root.childStack.value.active.instance,
            ).component.onConfigureProject()
        }
        withTimeout(10_000) {
            while (updates.receive().loading) { /* Await the initial project read. */ }
            dependencies.projectsRepository.deleteProject(project.id)
            while (true) {
                val state = updates.receive()
                if (state.selectedId == null && state.projects.isEmpty()) break
            }
        }
        runOnUiThread {
            val child =
                assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
            assertEquals(emptyList(), child.component.projectPath)
            assertEquals(1, root.childStack.value.items.size)
        }
        updates.close()
    }
}
