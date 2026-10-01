package ankideckbuilder.ui.compose.projects

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import ankideckbuilder.testing.GoldenScreenshotTest
import ankideckbuilder.ui.components.projects.ProjectsComponent
import ankideckbuilder.ui.components.projects.TestProjectsComponent
import kotlin.test.Test

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
