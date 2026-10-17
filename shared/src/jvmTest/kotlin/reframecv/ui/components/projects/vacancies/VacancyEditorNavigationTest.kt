package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode
import reframecv.repository.ProjectsRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.components.application.RootComponent
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ProjectsUseCases

class VacancyEditorNavigationTest : ComponentTest() {
    @Test
    fun editorOwnsDraftAndBackPopsOnlyEditorPage() = runOnUiThread {
        lifecycle.resume()
        var backs = 0
        val component = DefaultVacanciesComponent(appComponentContext(), 1, back = { backs++ })
        component.onAdd()
        val editor = assertIs<DefaultVacancyEditorComponent>(component.editor)
        assertEquals(2, component.childStack.value.items.size)
        val draft = VacancyDraft("Backend", description = "Original text")
        editor.onDraftChange(draft)
        assertEquals(draft, editor.draft.value)
        editor.saveState.value = VacancySaveState.Saving
        editor.onDraftChange(VacancyDraft("Changed"))
        component.onBack()
        assertEquals(draft, editor.draft.value)
        assertSame(editor, component.editor)
        editor.saveState.value = VacancySaveState.Idle
        component.onBack()
        assertNull(component.editor)
        assertEquals(1, component.childStack.value.items.size)
        assertEquals(0, backs)
        component.onBack()
        assertEquals(1, backs)
        component.onAdd()
        assertEquals(VacancyDraft(""), component.editor?.draft?.value)
    }

    @Test
    fun treeAndHardwareBackCannotLeaveWhileWriting() = runBlocking<Unit> {
        val projects = listOf(1L, 2L).map {
            Project(
                it,
                "Project $it",
                createdAt = 100,
                updatedAt = 100,
                mode = ProjectMode.Workspace,
            )
        }
        val services = object : ApplicationDependencies by dependencies {
            override val projectsUseCases get() = ProjectsUseCases(projectsRepository)
            override val projectsRepository =
                object : ProjectsRepository by dependencies.projectsRepository {
                    override fun observeAllProjects() = flowOf(projects)
                }
        }
        val ready = CompletableDeferred<Unit>()
        lateinit var root: DefaultRootComponent
        runOnUiThread {
            lifecycle.resume()
            root =
                DefaultRootComponent(
                    DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
                )
            root.projectTree.state.subscribe { if (it.projects.size == 2) ready.complete(Unit) }
        }
        withTimeout(10_000) { ready.await() }
        runOnUiThread {
            root.projectTree.onProjectSelected(1)
            assertIs<RootComponent.Child.Dashboard>(
                root.childStack.value.active.instance,
            ).component.onOpenVacancies()
            val vacancies =
                assertIs<RootComponent.Child.Vacancies>(root.childStack.value.active.instance)
            vacancies.component.onAdd()
            val editor = assertIs<DefaultVacancyEditorComponent>(vacancies.component.editor)
            for (deleting in listOf(false, true)) {
                editor.saveState.value =
                    if (deleting) VacancySaveState.Idle else VacancySaveState.Saving
                editor.deleteState.value =
                    if (deleting) VacancyDeleteState.Deleting else VacancyDeleteState.Idle
                root.onProjectsList()
                root.projectTree.onProjectSelected(2)
                (root.backHandler as BackDispatcher).back()
                assertSame(vacancies, root.childStack.value.active.instance)
                assertSame(editor, vacancies.component.editor)
                assertEquals(1L, root.projectTree.state.value.selectedId)
            }
            editor.deleteState.value = VacancyDeleteState.Idle
            (root.backHandler as BackDispatcher).back()
            assertNull(vacancies.component.editor)
            assertSame(vacancies, root.childStack.value.active.instance)
            (root.backHandler as BackDispatcher).back()
            assertIs<RootComponent.Child.Dashboard>(root.childStack.value.active.instance)
        }
    }
}
