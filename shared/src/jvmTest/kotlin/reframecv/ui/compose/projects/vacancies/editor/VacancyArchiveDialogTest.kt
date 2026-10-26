package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportFormat
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState

@OptIn(ExperimentalTestApi::class)
class VacancyArchiveDialogTest : GoldenScreenshotTest() {
    @Test
    fun archiveFieldsSelectDocumentsAndKeepDownloadVisibleWhileScrolling() =
        runDesktopComposeUiTest {
            val component = archiveExportFixture()
            setGoldenContent(this) { VacancyExportDialog(component) {} }
            onNodeWithTag("vacancy.export.archive.name").performTextReplacement("Custom name")
            onNodeWithTag(
                "vacancy.export.archive.resume.1",
            ).performScrollTo().assertIsOn().performClick()
            onNodeWithTag("vacancy.export.archive.letter.2").performScrollTo().performClick()
            assertEquals("Custom name", component.archive.value.name)
            assertEquals(setOf(2L), component.archive.value.resumeIds)
            assertEquals(setOf(1L), component.archive.value.letterIds)
            onNodeWithText("Download").assertIsDisplayed()
            onNodeWithTag(
                "vacancy.export.archive.name",
            ).performScrollTo().performTextReplacement(" ... ")
            onNodeWithText("Download").assertIsNotEnabled()
            runOnIdle { component.state.value = VacancyExportState.Exporting }
            onNodeWithTag("vacancy.export.archive.name").assertIsNotEnabled()
            onNodeWithTag("vacancy.export.cancel").assertIsNotEnabled()
        }

    @Test
    fun selectingZipExpandsTheExistingDialogAndCancelOrEscapeClosesItCompletely() =
        runDesktopComposeUiTest {
            val editor = TestVacancyEditorComponent(initialVacancy = Vacancy(7, 1, "Example", 0, 0))
            setGoldenContent(this) { VacancyEditorContent(editor) }
            onNodeWithText("Export").performClick()
            onNodeWithTag("vacancy.export.format").performClick()
            onNodeWithTag("vacancy.export.zip").performClick()
            onNodeWithTag("vacancy.export.archive.name").assertIsDisplayed()
            onNodeWithTag("vacancy.export.cancel").performClick()
            onNodeWithTag("vacancy.export.dialog").assertDoesNotExist()
            assertEquals(VacancyExportFormat.Pdf, editor.exportComponent.format.value)
            assertFalse(editor.exportComponent.archive.value.loaded)
            onNodeWithText("Export").performClick()
            onNodeWithTag("vacancy.export.format").performClick()
            onNodeWithTag("vacancy.export.zip").performClick()
            onNodeWithTag("vacancy.export.dialog").performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("vacancy.export.dialog").assertDoesNotExist()
            onNodeWithTag(VACANCY_EDITOR_TAG).assertExists()
        }
}
