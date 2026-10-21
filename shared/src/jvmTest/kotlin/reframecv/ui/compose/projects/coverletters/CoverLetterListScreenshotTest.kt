package reframecv.ui.compose.projects.coverletters

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.coverletters.CoverLettersState
import reframecv.ui.components.projects.coverletters.TestCoverLettersComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class CoverLetterListScreenshotTest : GoldenScreenshotTest() {
    private fun fixture() = TestCoverLettersComponent(initialState = letterListSamples()).also {
        it.selection.onToggle(1)
    }

    @Test
    fun linkedVacanciesAndSelectionMatchReference() = runComposeUiTest {
        setAndCaptureGolden(this) { CoverLettersContent(fixture()) }
    }

    @Test
    fun narrowListMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(320.dp, 600.dp)) {
            CoverLettersContent(fixture())
        }
    }

    @Test
    fun bulkConfirmationInLightThemeMatchesReference() = runComposeUiTest {
        val component = fixture().also { it.selection.onRequestDelete() }
        setAndCaptureGolden(
            this,
            themeMode = ReframeTheme.ThemeMode.Light,
            captureTag = "coverletters.delete.dialog",
        ) { CoverLettersContent(component) }
    }

    @Test
    fun emptyListMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this) { CoverLettersContent(TestCoverLettersComponent()) }
    }

    @Test
    fun failedListMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this) {
            CoverLettersContent(
                TestCoverLettersComponent(initialState = CoverLettersState.LoadFailed),
            )
        }
    }
}
