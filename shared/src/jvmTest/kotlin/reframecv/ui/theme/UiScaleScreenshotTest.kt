package reframecv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.compose.projects.common.ProjectBreadcrumbs
import reframecv.ui.compose.projects.list.ProjectAction
import reframecv.ui.compose.projects.list.ProjectControls
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class UiScaleScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun scaleGalleryInDarkThemeMatchesReference() = runComposeUiTest {
        captureGallery(this, ThemeMode.Dark)
    }

    @Test
    fun scaleGalleryInLightThemeMatchesReference() = runComposeUiTest {
        captureGallery(this, ThemeMode.Light)
    }

    private fun captureGallery(test: ComposeUiTest, themeMode: ThemeMode) {
        setAndCaptureGolden(test, themeMode = themeMode) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                for (percent in listOf(75, 100, 150)) {
                    ReframeTheme(themeMode = themeMode, uiScale = UiScale(percent)) {
                        Column {
                            Text(
                                "Scale $percent%",
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            ProjectBreadcrumbs(listOf(ProjectBreadcrumb(1, "Android Developer")))
                            ProjectControls(listOf(ProjectAction("Add Project", onClick = {})))
                        }
                    }
                }
            }
        }
    }
}
