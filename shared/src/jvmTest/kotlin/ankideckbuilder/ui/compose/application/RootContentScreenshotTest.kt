package ankideckbuilder.ui.compose.application

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import ankideckbuilder.testing.GoldenScreenshotTest
import ankideckbuilder.ui.components.application.TestRootComponent
import kotlin.test.Test

class RootContentScreenshotTest : GoldenScreenshotTest() {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyProjectsStateMatchesReference() = runComposeUiTest {
        val component = TestRootComponent()
        setAndCaptureGolden(this) { RootContent(component) }
    }
}
