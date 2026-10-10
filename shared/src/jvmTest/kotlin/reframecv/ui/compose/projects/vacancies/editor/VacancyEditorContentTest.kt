package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyEditorContentTest {
    @Test
    fun requiresTitleAndSubmitsAllFieldsWithEnter() = runComposeUiTest {
        val drafts = mutableListOf<VacancyDraft>()
        val component = TestVacancyEditorComponent(save = { drafts += it })
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithTag("vacancy.title").assertIsFocused()
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithTag("vacancy.title").performTextReplacement("   ")
        onNodeWithTag("vacancy.title").performImeAction()
        assertEquals(emptyList(), drafts)
        onNodeWithTag("vacancy.title").performTextReplacement("Android Developer")
        onNodeWithTag("vacancy.company").performTextReplacement("Example")
        onNodeWithTag("vacancy.description").performTextReplacement("Job description\nRequirements")
        onNodeWithTag("vacancy.url").performScrollTo().performTextReplacement("https://example.com")
        onNodeWithTag(
            "vacancy.keywords",
        ).performScrollTo().performTextReplacement("Kotlin, Android")
        onNodeWithTag("vacancy.keywords").performImeAction()
        assertEquals(
            listOf(
                VacancyDraft(
                    "Android Developer",
                    "Example",
                    "Job description\nRequirements",
                    "https://example.com",
                    "Kotlin, Android",
                ),
            ),
            drafts,
        )
    }

    @Test
    fun keepsDraftAfterFailureAndBlocksActionsWhileSaving() = runComposeUiTest {
        var closes = 0
        var saves = 0
        val component = TestVacancyEditorComponent(save = { saves++ }, close = { closes++ })
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithTag("vacancy.title").performTextReplacement("Backend")
        onNodeWithText("Save").performClick()
        assertEquals(1, saves)
        runOnIdle { component.saveState.value = VacancySaveState.Failed }
        onNodeWithText("Could not save the vacancy. Please try again.").performScrollTo()
        onNodeWithTag("vacancy.title").performScrollTo()
        onNodeWithText("Backend").assertExists()
        onNodeWithText("Save").assertIsEnabled()
        runOnIdle { component.saveState.value = VacancySaveState.Saving }
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithTag("vacancy.title").assertIsNotEnabled()
        runOnIdle { component.saveState.value = VacancySaveState.Idle }
        onNodeWithText("Cancel").performClick()
        assertEquals(1, closes)
    }

    @Test
    fun editingPrefillsFieldsAndCancelDoesNotSubmitChanges() = runComposeUiTest {
        val original = Vacancy(
            id = 7, projectId = 1, name = "Backend", company = "Example",
            description = "Job description", url = "https://example.com",
            keywords = listOf(
                "Kotlin",
                "Java",
            ),
            createdAt = 100, updatedAt = 200,
        )
        var closes = 0
        val saved = mutableListOf<VacancyDraft>()
        val component = TestVacancyEditorComponent(
            initialVacancy = original,
            save = { saved += it },
            close = { closes++ },
        )
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithText("Edit vacancy").assertExists()
        onNodeWithText("Backend").assertExists()
        onNodeWithText("Example").assertExists()
        onNodeWithText("Job description").assertExists()
        onNodeWithText("https://example.com").assertExists()
        onNodeWithText("Kotlin, Java").assertExists()
        onNodeWithTag("vacancy.title").assertIsFocused().performTextReplacement("Changed")
        onNodeWithText("Cancel").performClick()
        assertEquals(1, closes)
        assertEquals(emptyList(), saved)
    }
}
