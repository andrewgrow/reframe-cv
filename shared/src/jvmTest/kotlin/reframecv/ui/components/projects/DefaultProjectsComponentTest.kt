package reframecv.ui.components.projects

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.project.Project
import reframecv.repository.ProjectsRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.threading.runOnUiThread

class DefaultProjectsComponentTest : ComponentTest() {
    @Test
    fun failedCreationKeepsEditorOpenAndDoesNotNavigate() = runBlocking {
        val states = Channel<EditorSaveState>(Channel.UNLIMITED)
        val opened = mutableListOf<Project>()
        val repository = object : ProjectsRepository by dependencies.projectsRepository {
            override suspend fun createProject(name: String, parentId: Long?): Project =
                error("Write failed")
        }
        lateinit var component: DefaultProjectsComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultProjectsComponent(
                appComponentContext(),
                onProjectOpened = opened::add,
                executorFactory = { RealProjectsExecutor(repository) },
            )
            component.onAddProject()
            val editor = assertNotNull(component.editorSlot.value.child).instance
            editor.saveState.subscribe { states.trySend(it) }
            editor.onSave("Backend")
        }
        withTimeout(10_000) {
            while (states.receive() != EditorSaveState.Failed) { /* Await write failure. */ }
        }
        runOnUiThread {
            assertNotNull(component.editorSlot.value.child)
            assertTrue(opened.isEmpty())
        }
    }

    @Test
    fun deletesAfterCaseInsensitiveConfirmationAndClosesEditorAfterSuccess() = runOnUiThread {
        lifecycle.resume()
        val executor = TestProjectsExecutor()
        val component =
            DefaultProjectsComponent(appComponentContext(), executorFactory = { executor })
        val project = Project(id = 7, name = "Android Developer", createdAt = 1, updatedAt = 1)
        component.onEditProject(project)
        val editor = assertNotNull(component.editorSlot.value.child).instance
        listOf("", "DELET", " DELETE", "DELETE ").forEach { editor.onDelete(it) }
        assertNotNull(component.editorSlot.value.child)
        assertEquals(ProjectsIntent.LoadProjects, executor.lastIntent)
        editor.onDelete("dElEtE")
        assertEquals(ProjectsIntent.DeleteProject(project.id), executor.lastIntent)
        assertNull(component.editorSlot.value.child)

        component.onAddProject()
        assertNotNull(component.editorSlot.value.child).instance.onDelete("DELETE")
        assertNotNull(component.editorSlot.value.child)
        assertEquals(ProjectsIntent.DeleteProject(project.id), executor.lastIntent)
    }

    @Test
    fun opensProjectForUpdateAndSavesItsIdWithTrimmedName() = runOnUiThread {
        lifecycle.resume()
        val executor = TestProjectsExecutor()
        val opened = mutableListOf<Project>()
        val component = DefaultProjectsComponent(
            appComponentContext(),
            executorFactory = { executor },
            onProjectOpened = opened::add,
        )
        val now = 1_000L
        val project = Project(id = 7, name = "Android Developer", createdAt = now, updatedAt = now)
        component.onEditProject(project)

        val editor = assertNotNull(component.editorSlot.value.child).instance
        assertEquals(project.name, editor.initialName)
        editor.onSave("  Backend Developer  ")
        assertEquals(
            ProjectsIntent.UpdateProject(project.id, "Backend Developer"),
            executor.lastIntent,
        )
        assertNull(component.editorSlot.value.child)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun savesNonBlankNameClosesEditorAndOpensCreatedProject() = runOnUiThread {
        lifecycle.resume()
        val opened = mutableListOf<Project>()
        val component = DefaultProjectsComponent(
            appComponentContext(),
            executorFactory = ::TestProjectsExecutor,
            onProjectOpened = opened::add,
        )
        component.onAddProject()
        val editor = assertNotNull(component.editorSlot.value.child).instance
        editor.onSave("   ")
        assertNotNull(component.editorSlot.value.child)
        assertTrue(opened.isEmpty())
        editor.onSave("  Android Developer  ")
        assertNull(component.editorSlot.value.child)
        assertEquals("Android Developer", opened.single().name)
        assertEquals(1L, opened.single().id)
    }

    @Test
    fun opensClosesAndRecreatesEditorWithChildLifecycle() = runOnUiThread {
        lifecycle.resume()
        val component = DefaultProjectsComponent(
            appComponentContext(),
            executorFactory = ::TestProjectsExecutor,
        )
        assertNull(component.editorSlot.value.child)

        component.onAddProject()
        val editor = assertNotNull(component.editorSlot.value.child).instance
        val editorLifecycle = (editor as LifecycleOwner).lifecycle
        assertEquals(Lifecycle.State.RESUMED, editorLifecycle.state)

        component.onAddProject()
        assertSame(editor, component.editorSlot.value.child?.instance)

        editor.onClose()
        assertNull(component.editorSlot.value.child)
        assertEquals(Lifecycle.State.DESTROYED, editorLifecycle.state)

        component.onAddProject()
        val reopened = assertNotNull(component.editorSlot.value.child).instance
        assertNotSame(editor, reopened)
        lifecycle.destroy()
        assertEquals(Lifecycle.State.DESTROYED, (reopened as LifecycleOwner).lifecycle.state)
    }

    @Test
    fun exposesInitialStateAndDisposesStoreWithComponent() {
        val expectedLoading = 1
        runOnUiThread {
            lifecycle.resume()
            val factory = RecordingStoreFactory()
            val executor = TestProjectsExecutor()
            val component = DefaultProjectsComponent(
                componentContext = appComponentContext(),
                storeFactory = factory,
                executorFactory = { executor },
            )

            assertEquals(UiState.NoProjects, factory.store.state)
            assertEquals(UiState.NoProjects, component.uiState.value)
            assertFalse(factory.store.isDisposed)
            assertEquals(expectedLoading, executor.loadCount)
            assertFalse(executor.isDisposed)

            lifecycle.pause()
            lifecycle.resume()
            assertEquals(expectedLoading, executor.loadCount)

            lifecycle.destroy()
            assertTrue(factory.store.isDisposed)
            assertTrue(executor.isDisposed)
        }
    }
}
