package reframecv.ui.compose.projects.coverletters

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.ui.components.projects.coverletters.CoverLetterExportFormat
import reframecv.ui.components.projects.coverletters.CoverLetterExportState
import reframecv.ui.components.projects.coverletters.TestCoverLetterExportComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class CoverLetterPdfContentTest {
    @Test
    fun pdfIsDefaultAndJsonCanBeSelectedWhileBusyDisablesChanges() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            var downloads = 0
            val component = TestCoverLetterExportComponent { downloads++ }
            setContent { ReframeTheme { CoverLetterExportDialog(component) {} } }
            onNodeWithTag("cover_letter.export.format").assertTextContains("as PDF").performClick()
            onNodeWithTag("cover_letter.export.json").performClick()
            assertEquals(CoverLetterExportFormat.Json, component.format.value)
            onNodeWithTag("cover_letter.export.format").assertTextContains("as JSON").performClick()
            onNodeWithTag("cover_letter.export.pdf").performClick()
            onNodeWithText("Download").performClick()
            assertEquals(1, downloads)
            runOnIdle { component.state.value = CoverLetterExportState.Exporting }
            onNodeWithTag("cover_letter.export.progress").assertExists()
            onNodeWithTag("cover_letter.export.format").assertIsNotEnabled()
            onNodeWithText("Download").assertIsNotEnabled()
            onNodeWithText("Cancel").assertIsNotEnabled()
        }
}
