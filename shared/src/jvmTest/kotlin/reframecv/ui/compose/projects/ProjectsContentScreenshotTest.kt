package reframecv.ui.compose.projects

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.TestProjectsComponent

@OptIn(ExperimentalTestApi::class)
class ProjectsContentScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun openProjectEditorMatchesReference() = runDesktopComposeUiTest {
        val component = TestProjectsComponent(editorInitiallyOpen = true)
        captureProjectsGolden(this, component)
    }

    @Test
    fun emptyProjectsStateMatchesReference() = runComposeUiTest {
        val component = TestProjectsComponent()
        captureProjectsGolden(this, component)
    }

    private fun captureProjectsGolden(composeUiTest: ComposeUiTest, component: ProjectsComponent) {
        setAndCaptureGolden(
            composeUiTest = composeUiTest,
            captureTag = PROJECTS_SCREEN_TAG,
        ) {
            ProjectsContent(component)
        }
    }
}
