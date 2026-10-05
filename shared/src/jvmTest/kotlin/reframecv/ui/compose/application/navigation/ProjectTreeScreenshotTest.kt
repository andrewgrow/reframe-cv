package reframecv.ui.compose.application.navigation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.application.navigation.TestProjectTreeComponent
import reframecv.ui.compose.application.RootNavigation
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProjectTreeScreenshotTest : GoldenScreenshotTest() {
    private val projects = listOf(
        Project(id = 1, name = "Backend", createdAt = 0, updatedAt = 0),
        Project(2, "Java", 0, 0, parentId = 1),
        Project(3, "Node.js", 0, 0, parentId = 1),
        Project(4, "Mobile", 0, 0),
        Project(5, "Android", 0, 0, parentId = 4),
        Project(6, "An exceptionally long project name for a company", 0, 0),
        Project(7, "Full-stack", 0, 0),
    )

    @Test
    fun expandedTreeInDarkThemeMatchesReference() = runDesktopComposeUiTest(
        ScreenSize(220.dp, 420.dp),
    ) {
        setGoldenContent(this, ScreenSize(220.dp, 420.dp)) {
            RootNavigation(component())
        }
        onNodeWithText("Java").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Full-stack").performMouseInput { enter(center) }
        captureGolden(this, "navigation.tree")
    }

    @Test
    fun expandedTreeInLightThemeMatchesReference() = runDesktopComposeUiTest(
        ScreenSize(220.dp, 420.dp),
    ) {
        setAndCaptureGolden(this, ScreenSize(220.dp, 420.dp), themeMode = ThemeMode.Light) {
            RootNavigation(component())
        }
    }

    private fun component() = TestProjectTreeComponent(
        ProjectTreeState(projects, expandedIds = setOf(1), selectedId = 2, loading = false),
    )
}
