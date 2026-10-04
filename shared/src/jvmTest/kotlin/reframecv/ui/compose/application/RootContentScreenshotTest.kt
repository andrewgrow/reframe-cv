package reframecv.ui.compose.application

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_projects_list
import reframecv.testing.GoldenScreenshotTest
import reframecv.testing.getTestString
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

class RootContentScreenshotTest : GoldenScreenshotTest() {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hoveredNavigationMatchesReference() = runComposeUiTest {
        setGoldenContent(this) { RootContent(TestRootComponent()) }
        onNodeWithText(getTestString(Res.string.navigation_projects_list))
            .performMouseInput { enter(center) }
        captureGolden(this)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hoveredNavigationInLightThemeMatchesReference() = runComposeUiTest {
        setGoldenContent(this, themeMode = ThemeMode.Light) { RootContent(TestRootComponent()) }
        onNodeWithText(getTestString(Res.string.navigation_projects_list))
            .performMouseInput { enter(center) }
        captureGolden(this)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun partiallyFilledNavigationMatchesReference() = runComposeUiTest {
        setGoldenContent(this) { RootContent(TestRootComponent()) }
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithText(getTestString(Res.string.navigation_projects_list))
            .performMouseInput { enter(center) }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(100)
        captureGolden(this)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun navigationAfterHoverExitMatchesReference() = runComposeUiTest {
        setGoldenContent(this) { RootContent(TestRootComponent()) }
        val navigation = onNodeWithText(getTestString(Res.string.navigation_projects_list))
        navigation.performMouseInput { enter(center) }
        waitForIdle()
        navigation.performMouseInput { exit() }
        captureGolden(this)
    }

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
