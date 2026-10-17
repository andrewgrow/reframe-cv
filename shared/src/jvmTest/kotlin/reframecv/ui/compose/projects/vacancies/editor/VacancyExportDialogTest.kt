package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.TestVacancyExportComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState

@OptIn(ExperimentalTestApi::class)
class VacancyExportDialogTest : GoldenScreenshotTest() {
    @Test
    fun onlySavedVacancyOffersExportAndCancelDownloadAndEscapeKeepThePageOpen() =
        runDesktopComposeUiTest {
            var saves = 0
            var closes = 0
            val component = TestVacancyEditorComponent(
                initialVacancy = Vacancy(
                    id = 7,
                    projectId = 1,
                    name = "Mobile",
                    createdAt = 0,
                    updatedAt = 0,
                ),
                save = { saves++ },
                close = { closes++ },
            )
            setGoldenContent(this) { VacancyEditorContent(component) }
            listOf("Cancel", "Download").forEach { action ->
                onNodeWithText("Export").performClick()
                onNodeWithTag("vacancy.export.format").assertTextContains("as JSON").performClick()
                onNodeWithTag("vacancy.export.json").performClick()
                onNode(
                    hasText(action) and hasAnyAncestor(hasTestTag("vacancy.export.dialog")),
                ).performClick()
                onNodeWithTag("vacancy.export.dialog").assertDoesNotExist()
                onNodeWithTag(VACANCY_EDITOR_TAG).assertExists()
            }
            onNodeWithText("Export").performClick()
            onNodeWithTag("vacancy.export.dialog").performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("vacancy.export.dialog").assertDoesNotExist()
            assertEquals(3, saves)
            assertEquals(0, closes)
        }

    @Test
    fun creationDoesNotOfferExport() = runDesktopComposeUiTest {
        setGoldenContent(this) { VacancyEditorContent(TestVacancyEditorComponent()) }
        onNodeWithText("Export").assertDoesNotExist()
    }

    @Test
    fun exportProgressBlocksActionsAndEscapeAndFailureAllowsRetry() = runDesktopComposeUiTest {
        var downloads = 0
        var closes = 0
        val component = TestVacancyExportComponent { downloads++ }
        component.state.value = VacancyExportState.Exporting
        setGoldenContent(this) { VacancyExportDialog(component) { closes++ } }
        onNodeWithTag("vacancy.export.progress").assertExists()
        onNodeWithTag("vacancy.export.format").assertIsNotEnabled()
        onNodeWithText("Download").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithTag("vacancy.export.dialog").performKeyInput { pressKey(Key.Escape) }
        assertEquals(0, closes)
        runOnIdle { component.state.value = VacancyExportState.Failed }
        onNodeWithText("Could not export the vacancy. Please try again.").assertExists()
        onNodeWithText("Download").performClick()
        assertEquals(1, downloads)
    }
}
