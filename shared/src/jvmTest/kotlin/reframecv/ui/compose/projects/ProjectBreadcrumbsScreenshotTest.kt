package reframecv.ui.compose.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProjectBreadcrumbsScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun inDarkThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Dark)
    }

    @Test
    fun inLightThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Light)
    }

    private fun captureComponent(test: ComposeUiTest, themeMode: ThemeMode) {
        setAndCaptureGolden(test, themeMode = themeMode) {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProjectBreadcrumbs(emptyList())
                ProjectBreadcrumbs(listOf(ProjectBreadcrumb(1, "Android Developer")))
                ProjectBreadcrumbs(
                    listOf(
                        ProjectBreadcrumb(1, "Career"),
                        ProjectBreadcrumb(2, "Android Developer"),
                    ),
                )
                ProjectBreadcrumbs(
                    List(6) { index ->
                        ProjectBreadcrumb(index.toLong(), "Parent project with a long name $index")
                    },
                )
            }
        }
    }
}
