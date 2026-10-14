package reframecv.ui.compose.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class FastScrollColumnScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun topMatchesReference() = capture(ThemeMode.Dark, 30)

    @Test
    fun bottomMatchesReference() = capture(ThemeMode.Dark, 30, bottom = true)

    @Test
    fun lightThemeMatchesReference() = capture(ThemeMode.Light, 30)

    @Test
    fun shortContentMatchesReference() = capture(ThemeMode.Light, 1)

    private fun capture(theme: ThemeMode, count: Int, bottom: Boolean = false) =
        runDesktopComposeUiTest(ScreenSize(320.dp, 400.dp)) {
            setGoldenContent(this, screenSize = ScreenSize(320.dp, 400.dp), themeMode = theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FastScrollColumn(scrollState = rememberScrollState()) {
                        repeat(count) { Text("Item ${it + 1}", Modifier.height(40.dp)) }
                    }
                }
            }
            if (bottom) onNodeWithTag("scroll.to.bottom").performClick()
            captureGolden(this)
        }
}
