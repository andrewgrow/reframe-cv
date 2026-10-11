package reframecv.ui.compose.application

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.application.RootComponent
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.dashboard.TestDashboardComponent
import reframecv.ui.components.projects.vacancies.TestVacanciesComponent
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class PageBackNavigationTest {
    @Test
    fun projectArrowAndEscapeWorkWithMenuFocus() = runComposeUiTest {
        var backs = 0
        val projects = TestProjectsComponent(
            projectPath = listOf(ProjectBreadcrumb(7, "Backend")),
            back = { backs++ },
        )
        setContent { ReframeTheme { RootContent(TestRootComponent(projectsComponent = projects)) } }
        onNodeWithText("Backend").assertExists()
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
        onNodeWithText("Projects").performClick()
        onNodeWithTag("app.root").performKeyInput { pressKey(Key.Escape) }
        assertEquals(2, backs)
    }

    @Test
    fun rootProjectListHasNoBackAction() = runComposeUiTest {
        var backs = 0
        setContent {
            ReframeTheme {
                RootContent(
                    TestRootComponent(
                        projectsComponent = TestProjectsComponent(back = {
                            backs++
                        }),
                    ),
                )
            }
        }
        onNodeWithContentDescription("Back").assertDoesNotExist()
        onNodeWithText("Projects").performClick()
        onNodeWithTag("app.root").performKeyInput { pressKey(Key.Escape) }
        assertEquals(0, backs)
    }

    @Test
    fun dashboardArrowAndEscapeWorkEvenWhenTheMenuHasFocus() = runComposeUiTest {
        var backs = 0
        val dashboard = TestDashboardComponent(back = { backs++ })
        setContent {
            ReframeTheme { RootContent(TestRootComponent(dashboardComponent = dashboard)) }
        }
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
        onNodeWithText("Projects").performClick()
        onNodeWithTag("app.root").performKeyInput { pressKey(Key.Escape) }
        assertEquals(2, backs)
    }

    @Test
    fun escapeCancelsDeleteConfirmationBeforeClosingTheEditor() = runComposeUiTest {
        var backs = 0
        val vacancies = TestVacanciesComponent(back = { backs++ })
        val vacancy = Vacancy(id = 1, projectId = 1, name = "Backend", createdAt = 0, updatedAt = 0)
        vacancies.onEdit(vacancy)
        setContent {
            ReframeTheme { RootContent(TestRootComponent(vacanciesComponent = vacancies)) }
        }
        onNodeWithText("Delete").performClick()
        onNodeWithTag("vacancies.editor").performKeyInput { pressKey(Key.Escape) }
        assertEquals(
            VacancyDeleteState.Idle,
            vacancies.editorSlot.value.child?.instance?.deleteState?.value,
        )
        onNodeWithText("Edit vacancy").assertExists()
        assertEquals(0, backs)
        onNodeWithTag("vacancies.editor").performKeyInput { pressKey(Key.Escape) }
        onNodeWithText("Edit vacancy").assertDoesNotExist()
        assertEquals(0, backs)
    }

    @Test
    fun escapeClosesEditorBeforeReturningThroughPagesAndPreservesBusyGuard() {
        val projectId = 7L
        val projects = RootComponent.Child.Projects(
            TestProjectsComponent(projectPath = listOf(ProjectBreadcrumb(projectId, "Backend"))),
        )
        val stack = MutableValue(ChildStack<Unit, RootComponent.Child>(Unit, projects))
        fun show(child: RootComponent.Child) {
            stack.value = ChildStack(Unit, child)
        }
        val dashboard = RootComponent.Child.Dashboard(
            TestDashboardComponent(back = {
                show(projects)
            }),
        )
        val vacancies = TestVacanciesComponent(back = { show(dashboard) })
        show(RootComponent.Child.Vacancies(vacancies))
        val root = object : RootComponent by TestRootComponent() {
            override val childStack = stack
        }
        runComposeUiTest {
            setContent { ReframeTheme { RootContent(root) } }
            onNodeWithText("Add").performClick()
            val editor = assertIs<TestVacancyEditorComponent>(
                vacancies.editorSlot.value.child?.instance,
            )
            runOnIdle { editor.saveState.value = VacancySaveState.Saving }
            onNodeWithTag("vacancy.title").performKeyInput { pressKey(Key.Escape) }
            assertEquals(editor, vacancies.editorSlot.value.child?.instance)
            runOnIdle { editor.saveState.value = VacancySaveState.Idle }
            onNodeWithTag("vacancy.title").performKeyInput { pressKey(Key.Escape) }
            onNodeWithText("Add vacancy").assertDoesNotExist()
            assertIs<RootComponent.Child.Vacancies>(root.childStack.value.active.instance)
            // Headless tests do not reactivate the native parent window after Dialog dismissal.
            onNodeWithTag("app.root").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            onNodeWithTag("app.root").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            assertIs<RootComponent.Child.Dashboard>(root.childStack.value.active.instance)
            onNodeWithText("Dashboard").assertExists()
            onNodeWithTag("app.root").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            val projects =
                assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
            assertEquals(projectId, projects.component.projectPath.last().id)
        }
    }
}
