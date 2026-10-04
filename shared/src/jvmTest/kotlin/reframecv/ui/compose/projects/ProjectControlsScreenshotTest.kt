package reframecv.ui.compose.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_add
import reframecv.testing.GoldenScreenshotTest
import reframecv.testing.getTestString
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProjectControlsScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun inDarkThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Dark)
    }

    @Test
    fun inLightThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Light)
    }

    @Test
    fun manyActionsInDarkThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Dark, manyActions = true)
    }

    @Test
    fun manyActionsInLightThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Light, manyActions = true)
    }

    private fun captureComponent(
        test: ComposeUiTest,
        themeMode: ThemeMode,
        manyActions: Boolean = false,
    ) {
        val labels = listOf(getTestString(Res.string.projects_add)) + if (manyActions) {
            listOf(
                "Create resume", "Import resume", "Export project", "Project settings",
                "Duplicate project", "Archive project", "Share project", "Delete project",
                "Manage templates", "Resume history", "Adapt to vacancy",
            )
        } else {
            emptyList()
        }
        val screenSize = ScreenSize(width = 600.dp, height = if (manyActions) 120.dp else 72.dp)
        setAndCaptureGolden(test, screenSize = screenSize, themeMode = themeMode) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                ProjectControls(actions = labels.map { ProjectAction(label = it, onClick = {}) })
            }
        }
    }
}
