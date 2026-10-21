package reframecv.ui.compose.projects.coverletters

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.coverletters.LetterNotice
import reframecv.ui.components.projects.coverletters.TestCoverLetterEditorComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class CoverLetterEditorScreenshotTest : GoldenScreenshotTest() {
    private val record =
        CoverLetter(
            1,
            7,
            "ExampleCo — Android application",
            0,
            0,
            content =
                "Dear hiring team,\n\nI am applying for the Senior Android Developer position. " +
                    "My Kotlin and Jetpack Compose experience includes " +
                    "production camera integration " +
                    "and automated testing.\n\nKind regards,\nAlex",
            keywords = listOf("Android", "ExampleCo"),
        )

    @Test
    fun letterManagementMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) {
            CoverLetterEditorContent(TestCoverLetterEditorComponent(record))
        }
    }

    @Test
    fun savedLetterInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = TestCoverLetterEditorComponent(record)
        component.state.value = component.state.value.copy(notice = LetterNotice.Saved)
        setAndCaptureGolden(this, themeMode = ReframeTheme.ThemeMode.Light) {
            CoverLetterEditorContent(component)
        }
    }

    @Test
    fun pdfSavedMatchesReference() = runDesktopComposeUiTest {
        val component = TestCoverLetterEditorComponent(record)
        component.state.value = component.state.value.copy(notice = LetterNotice.PdfSaved)
        setAndCaptureGolden(this) { CoverLetterEditorContent(component) }
    }

    @Test
    fun letterDeletionConfirmationMatchesReference() = runDesktopComposeUiTest {
        val component = TestCoverLetterEditorComponent(record)
        component.onDelete()
        setAndCaptureGolden(this, captureTag = "confirmation.dialog") {
            CoverLetterEditorContent(component)
        }
    }
}
