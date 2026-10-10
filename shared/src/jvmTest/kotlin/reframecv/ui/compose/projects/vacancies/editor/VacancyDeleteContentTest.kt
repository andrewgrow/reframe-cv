package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyDeleteContentTest {
    private fun vacancy() =
        Vacancy(id = 12, projectId = 7, name = "Backend", createdAt = 100, updatedAt = 200)

    @Test
    fun confirmsDeletionWithEmptyTitleAndCancelReturnsToEditing() = runComposeUiTest {
        var deletions = 0
        val component =
            TestVacancyEditorComponent(initialVacancy = vacancy(), delete = { deletions++ })
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithTag("vacancy.title").performTextReplacement("")
        val delete = onNodeWithText("Delete").fetchSemanticsNode().boundsInRoot
        val cancel = onNodeWithText("Cancel").fetchSemanticsNode().boundsInRoot
        kotlin.test.assertTrue(delete.right < cancel.left)
        onNodeWithText("Delete").performClick()
        onNodeWithText("Delete this vacancy permanently?").assertIsDisplayed()
        onNodeWithText("Save").assertDoesNotExist()
        assertEquals(0, deletions)
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Delete this vacancy permanently?").assertDoesNotExist()
        onNodeWithText("Delete").assertIsDisplayed().performClick()
        onNodeWithText("Yes, delete this").assertIsEnabled().performClick()
        assertEquals(1, deletions)
    }

    @Test
    fun blocksActionsDuringDeletionAndShowsRetryAfterFailure() = runComposeUiTest {
        val component = TestVacancyEditorComponent(initialVacancy = vacancy())
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithText("Delete").performClick()
        runOnIdle { component.deleteState.value = VacancyDeleteState.Deleting }
        onNodeWithText("Yes, delete this").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithTag("vacancy.title").assertIsNotEnabled()
        runOnIdle { component.deleteState.value = VacancyDeleteState.Failed }
        onNodeWithText("Could not delete the vacancy. Please try again.").assertIsDisplayed()
        onNodeWithText("Yes, delete this").assertIsEnabled()
    }

    @Test
    fun creationHasNoDeleteButton() = runComposeUiTest {
        setContent { ReframeTheme { VacancyEditorContent(TestVacancyEditorComponent()) } }
        onNodeWithText("Delete").assertDoesNotExist()
    }
}
