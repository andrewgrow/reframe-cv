package reframecv.ui.compose.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProjectListScreenshotTest : GoldenScreenshotTest() {
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
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                ProjectList(
                    projects = List(30) { index ->
                        Project(
                            id = index.toLong(),
                            name = if (index == 0) "Android Developer" else "Resume project $index",
                            createdAt = 0,
                            updatedAt = 0,
                        )
                    },
                    onProjectClick = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
