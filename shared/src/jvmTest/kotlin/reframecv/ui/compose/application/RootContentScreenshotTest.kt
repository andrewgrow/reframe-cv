package reframecv.ui.compose.application

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

class RootContentScreenshotTest : GoldenScreenshotTest() {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyProjectsStateInLightThemeMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) { RootContent(TestRootComponent()) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyProjectsStateMatchesReference() = runComposeUiTest {
        val component = TestRootComponent()
        setAndCaptureGolden(this) { RootContent(component) }
    }
}
