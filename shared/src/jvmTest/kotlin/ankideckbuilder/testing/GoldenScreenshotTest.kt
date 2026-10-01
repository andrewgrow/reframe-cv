package ankideckbuilder.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest as runDesktopTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.rules.TestName

/**
 * JUnit 4 Compose screenshot helpers with a shared theme and screen size.
 * Golden paths are `package/Class/method.png`, without `ankideckbuilder.` and `ScreenshotTest`.
 * Each test method has one golden path; repeated captures reuse it.
 */
abstract class GoldenScreenshotTest {
    /** Requested content size in dp, subject to the test host's layout constraints. */
    data class ScreenSize(val width: Dp, val height: Dp)

    companion object {
        internal val defaultScreen = ScreenSize(800.dp, 600.dp)
    }

    @get:Rule
    val testName = TestName()

    /** Runs a desktop Compose test with the requested host screen size. */
    @OptIn(ExperimentalTestApi::class)
    protected fun runDesktopComposeUiTest(
        screenSize: ScreenSize = defaultScreen,
        block: suspend DesktopComposeUiTest.() -> Unit,
    ) = runDesktopTest(
        width = screenSize.width.value.toInt(),
        height = screenSize.height.value.toInt(),
        block = block,
    )

    /** Calls [setGoldenContent] then [captureGolden] for tests without intermediate actions. */
    @OptIn(ExperimentalTestApi::class)
    protected fun setAndCaptureGolden(
        composeUiTest: ComposeUiTest,
        screenSize: ScreenSize = defaultScreen,
        captureTag: String? = null,
        content: @Composable () -> Unit,
    ) {
        setGoldenContent(composeUiTest, screenSize, content)
        captureGolden(composeUiTest, captureTag)
    }

    /**
     * Sets themed content at [screenSize] size without capturing. Call once per Compose test scope,
     * then perform any actions before [captureGolden].
     */
    @OptIn(ExperimentalTestApi::class)
    protected fun setGoldenContent(
        composeUiTest: ComposeUiTest,
        screenSize: ScreenSize = defaultScreen,
        content: @Composable () -> Unit,
    ) {
        composeUiTest.setContent {
            MaterialTheme {
                Box(Modifier.size(width = screenSize.width, height = screenSize.height)) {
                    content()
                }
            }
        }
    }

    /**
     * Waits for Compose idleness and captures existing content during a running JUnit test.
     * Captures [captureTag] when provided, otherwise the single root.
     * For asynchronous loading, wait for the expected UI state before calling this method.
     */
    @OptIn(ExperimentalTestApi::class)
    protected fun captureGolden(composeUiTest: ComposeUiTest, captureTag: String? = null) {
        composeUiTest.waitForIdle()
        val componentPath = this@GoldenScreenshotTest.javaClass.name
            .removePrefix("ankideckbuilder.")
            .removeSuffix("ScreenshotTest")
            .replace('.', '/')
        val methodName = checkNotNull(testName.methodName) {
            "captureGolden() must be called from a running JUnit test."
        }
        val matcher = captureTag?.let { hasTestTag(it) } ?: isRoot()
        composeUiTest.onNode(matcher).captureRoboImage("$componentPath/$methodName.png")
    }
}
