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
import reframecv.domain.models.project.Project
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class DefaultProjectsComponentTest : ComponentTest() {
    @Test
    fun deletesAfterCaseInsensitiveConfirmationAndClosesEditorAfterSuccess() = runOnUiThread {
        lifecycle.resume()
        val executor = TestProjectsExecutor()
        val component =
            DefaultProjectsComponent(appComponentContext(), executorFactory = { executor })
        val project = Project(id = 7, name = "Android Developer", createdAt = 1, updatedAt = 1)
        component.onProjectClick(project)
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
        val component =
            DefaultProjectsComponent(appComponentContext(), executorFactory = { executor })
        val now = 1_000L
        val project = Project(id = 7, name = "Android Developer", createdAt = now, updatedAt = now)
        component.onProjectClick(project)

        val editor = assertNotNull(component.editorSlot.value.child).instance
        assertEquals(project.name, editor.initialName)
        editor.onSave("  Backend Developer  ")
        assertEquals(
            ProjectsIntent.UpdateProject(project.id, "Backend Developer"),
            executor.lastIntent,
        )
        assertNull(component.editorSlot.value.child)
    }

    @Test
    fun savesNonBlankNameAndClosesEditorAfterSuccess() = runOnUiThread {
        lifecycle.resume()
        val component = DefaultProjectsComponent(
            appComponentContext(),
            executorFactory = ::TestProjectsExecutor,
        )
        component.onAddProject()
        val editor = assertNotNull(component.editorSlot.value.child).instance
        editor.onSave("   ")
        assertNotNull(component.editorSlot.value.child)
        editor.onSave("  Android Developer  ")
        assertNull(component.editorSlot.value.child)
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
