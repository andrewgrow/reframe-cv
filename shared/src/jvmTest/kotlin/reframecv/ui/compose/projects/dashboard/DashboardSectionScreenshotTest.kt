package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class DashboardSectionScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun populatedSectionMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(320.dp, 240.dp)) {
            DashboardSection(
                title = "Resumes",
                entries = listOf(
                    DashboardEntry(1, "Android Developer"),
                    DashboardEntry(
                        2,
                        "Senior Kotlin Multiplatform Developer with a long resume title",
                    ),
                    DashboardEntry(3, "Backend Developer"),
                ),
                backgroundColor = ReframeTheme.colorScheme.dashboardResumes,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun emptySectionInLightThemeMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(
            this,
            screenSize = ScreenSize(320.dp, 240.dp),
            themeMode = ThemeMode.Light,
        ) {
            DashboardSection(
                title = "Cover letters",
                entries = emptyList(),
                backgroundColor = ReframeTheme.colorScheme.dashboardCoverLetters,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
