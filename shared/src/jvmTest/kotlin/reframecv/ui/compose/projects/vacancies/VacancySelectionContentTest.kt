package reframecv.ui.compose.projects.vacancies

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.TimeZone
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_delete_confirm
import reframecv.testing.getTestString
import reframecv.ui.components.projects.vacancies.TestVacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesState
import reframecv.ui.compose.common.documentDate
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancySelectionContentTest {
    @Test
    fun checkboxesSelectWithoutOpeningEditorAndDeletionRequiresConfirmation() = runComposeUiTest {
        val component =
            TestVacanciesComponent(initialState = VacanciesState.Ready(vacancySamples()))
        setContent { ReframeTheme { VacanciesContent(component) } }
        onNodeWithTag("vacancies.applied.selected").assertIsNotEnabled()
        onNodeWithTag("vacancies.delete.selected").assertIsNotEnabled()
        onNodeWithTag("vacancies.select.1").performClick().assertIsOn()
        onNodeWithTag("vacancies.applied.selected").assertIsEnabled().performClick()
        assertNull(component.editor)
        assertEquals(setOf(1L), component.selection.state.value.selected)
        onNodeWithTag("vacancies.delete.selected").assertIsEnabled().performClick()
        onNodeWithTag("vacancies.delete.dialog").assertExists()
        onNodeWithTag("confirmation.input").assertDoesNotExist()
        onNode(
            hasText("Cancel") and hasAnyAncestor(hasTestTag("vacancies.delete.dialog")),
        ).performClick()
        onNodeWithTag("vacancies.delete.dialog").assertDoesNotExist()
        assertEquals(setOf(1L), component.selection.state.value.selected)
        onNodeWithTag("vacancies.delete.selected").performClick()
        onNode(
            hasText(getTestString(Res.string.resume_delete_confirm)) and
                hasAnyAncestor(hasTestTag("vacancies.delete.dialog")),
        ).performClick()
        onNodeWithTag("vacancies.delete.dialog").assertDoesNotExist()
        onNodeWithTag("vacancies.delete.selected").assertIsNotEnabled()
    }

    @Test
    fun appliedDateUsesShortLocalFormatAndUnappliedRowsRemainEmpty() = runComposeUiTest {
        val records = vacancySamples()
        setContent {
            ReframeTheme {
                VacanciesContent(
                    TestVacanciesComponent(initialState = VacanciesState.Ready(records)),
                )
            }
        }
        val date =
            documentDate(checkNotNull(records.first().appliedAt), TimeZone.currentSystemDefault())
        onNodeWithTag("vacancies.applied.1", useUnmergedTree = true).assertTextEquals("✓ $date")
        onNodeWithTag("vacancies.applied.2", useUnmergedTree = true).assertTextEquals("")
    }
}
