package reframecv.ui.compose.application

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.project.Project
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.components.projects.resumes.TestResumesComponent
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent
import reframecv.ui.components.projects.vacancies.TestVacanciesComponent
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyImportState
import reframecv.ui.compose.common.documentFooterMinHeight
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.UiScale

@OptIn(ExperimentalTestApi::class)
class FooterAlignmentTest {
    @Test
    fun editorsAndNotificationsAlignAtDefaultScale() = checkEditors(100)

    @Test
    fun editorsAndNotificationsAlignAtLargerScale() = checkEditors(150)

    private fun checkEditors(percent: Int) = runDesktopComposeUiTest(width = 1000, height = 800) {
        var root by mutableStateOf(
            TestRootComponent(
                projectsComponent = TestProjectsComponent(
                    initialState = UiState.Projects(
                        listOf(Project(id = 1, name = "Project", createdAt = 0, updatedAt = 0)),
                    ),
                ),
            ),
        )
        setContent { ReframeTheme(uiScale = UiScale(percent)) { RootContent(root) } }
        assertAligned("projects.footer")
        val listHeight = onNodeWithTag("navigation.footer").fetchSemanticsNode().size.height
        val vacancies = TestVacanciesComponent().also { it.onAdd() }
        runOnIdle { root = TestRootComponent(vacanciesComponent = vacancies) }
        assertAligned("document.footer")
        val vacancyHeight = onNodeWithTag("document.footer").fetchSemanticsNode().size.height
        runOnIdle {
            val editor = vacancies.editor as TestVacancyEditorComponent
            editor.importComponent.state.value = VacancyImportState.Failed
        }
        assertAligned("document.footer")
        assertTrue(
            onNodeWithTag("document.footer").fetchSemanticsNode().size.height >= vacancyHeight,
        )
        runOnIdle { vacancies.editor!!.onClose() }
        waitForIdle()
        assertEquals(
            listHeight,
            onNodeWithTag("navigation.footer").fetchSemanticsNode().size.height,
        )
        val resume = TestResumeEditorComponent(initialResume = resume())
        runOnIdle {
            root =
                TestRootComponent(resumesComponent = TestResumesComponent(initialEditor = resume))
        }
        assertAligned("document.footer")
        assertEquals(
            vacancyHeight,
            onNodeWithTag("document.footer").fetchSemanticsNode().size.height,
        )
        runOnIdle { resume.saveState.value = ResumeSaveState.Failed }
        assertAligned("document.footer")
        assertEquals(
            vacancyHeight,
            onNodeWithTag("document.footer").fetchSemanticsNode().size.height,
        )
    }

    @Test
    fun navigationGrowsWithWrappedResumeActions() =
        runDesktopComposeUiTest(width = 600, height = 600) {
            var baseline = 0f
            val root = TestRootComponent(
                resumesComponent = TestResumesComponent(
                    initialEditor = TestResumeEditorComponent(initialResume = resume()),
                ),
            )
            setContent {
                ReframeTheme {
                    val height = documentFooterMinHeight()
                    baseline = with(LocalDensity.current) { height.toPx() }
                    RootContent(root)
                }
            }
            assertAligned("document.footer")
            assertTrue(onNodeWithTag("document.footer").fetchSemanticsNode().size.height > baseline)
        }

    @Test
    fun navigationFollowsInlineVacancyDeletionMessages() = runDesktopComposeUiTest {
        val vacancies = TestVacanciesComponent().also {
            it.onEdit(Vacancy(id = 1, projectId = 1, name = "Role", createdAt = 0, updatedAt = 0))
        }
        setContent {
            ReframeTheme { RootContent(TestRootComponent(vacanciesComponent = vacancies)) }
        }
        assertAligned("document.footer")
        runOnIdle {
            (vacancies.editor as TestVacancyEditorComponent).deleteState.value =
                reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState.Failed
        }
        assertAligned("document.footer")
    }

    private fun DesktopComposeUiTest.assertAligned(tag: String) {
        waitForIdle()
        val navigation = onNodeWithTag("navigation.footer").fetchSemanticsNode().boundsInRoot
        val controls = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertEquals(controls.top, navigation.top, absoluteTolerance = 1f)
        assertEquals(controls.height, navigation.height, absoluteTolerance = 1f)
    }

    private fun resume() = Resume(id = 1, projectId = 1, name = "CV", createdAt = 0, updatedAt = 0)
}
