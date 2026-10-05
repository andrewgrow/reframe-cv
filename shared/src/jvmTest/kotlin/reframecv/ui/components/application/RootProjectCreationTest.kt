package reframecv.ui.components.application

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.threading.runOnUiThread

class RootProjectCreationTest : ComponentTest() {
    @Test
    fun successfulCreationOpensTheProjectAndItsSubprojects() = runBlocking {
        lateinit var root: DefaultRootComponent
        val screens = Channel<ProjectsComponent>(Channel.UNLIMITED)
        val subscription = runOnUiThread {
            root = DefaultRootComponent(appComponentContext())
            lifecycle.resume()
            root.childStack.subscribe {
                screens.trySend(
                    assertIs<RootComponent.Child.Projects>(it.active.instance).component,
                )
            }
        }
        try {
            withTimeout(10_000) {
                val parent = createAndAwait(root, screens, "Backend", 1)
                val parentId = parent.projectPath.last().id
                assertEquals(parentId, root.projectTree.state.value.selectedId)
                val child = createAndAwait(root, screens, "Java", 2)
                val childId = child.projectPath.last().id
                assertEquals(listOf(parentId, childId), child.projectPath.map { it.id })
                assertEquals(childId, root.projectTree.state.value.selectedId)
                assertEquals(
                    childId,
                    dependencies.projectsRepository.observeProjects(parentId).first().single().id,
                )
                runOnUiThread {
                    assertNull(parent.editorSlot.value.child)
                    root.projectTree.onProjectSelected(parentId)
                    val returned = assertIs<RootComponent.Child.Projects>(
                        root.childStack.value.active.instance,
                    ).component
                    assertEquals(parentId, returned.projectPath.last().id)
                    root.projectTree.onProjectsList()
                    val rootList = assertIs<RootComponent.Child.Projects>(
                        root.childStack.value.active.instance,
                    ).component
                    assertEquals(emptyList(), rootList.projectPath)
                    assertNull(rootList.editorSlot.value.child)
                }
            }
        } finally {
            runOnUiThread { subscription.cancel() }
            screens.close()
        }
    }

    private suspend fun createAndAwait(
        root: RootComponent,
        screens: Channel<ProjectsComponent>,
        name: String,
        depth: Int,
    ): ProjectsComponent {
        runOnUiThread {
            val component = assertIs<RootComponent.Child.Projects>(
                root.childStack.value.active.instance,
            ).component
            component.onAddProject()
            assertNotNull(component.editorSlot.value.child).instance.onSave("  $name  ")
        }
        while (true) {
            val component = screens.receive()
            if (component.projectPath.size == depth &&
                component.projectPath.last().name == name
            ) {
                return component
            }
        }
    }
}
