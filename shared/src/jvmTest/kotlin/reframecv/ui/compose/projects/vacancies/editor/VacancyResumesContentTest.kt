package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyResumesState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyResumesContentTest {
    @Test
    fun filtersNamesAndKeywordsAndPreventsDuplicates() = runDesktopComposeUiTest {
        val editor = showResumes()
        onNodeWithTag("vacancy.resumes.select").performClick().performTextReplacement("KoTL")
        onNodeWithTag("vacancy.resumes.option.2").assertDoesNotExist()
        onNodeWithTag("vacancy.resumes.option.1").assertExists().performClick()
        assertEquals(listOf(VacancyResume(1)), editor.draft.value.resumes)
        onNodeWithTag("vacancy.resumes.score.1").assertExists()
        onNodeWithTag("vacancy.resumes.assess.1").performClick()
        assertEquals(listOf(VacancyResume(1)), editor.draft.value.resumes)
        onNodeWithTag("vacancy.resumes.select").performClick().performTextReplacement("CV")
        onNodeWithTag("vacancy.resumes.option.1").assertDoesNotExist()
        onNodeWithTag("vacancy.resumes.option.2").performClick()
        assertEquals(listOf(VacancyResume(1), VacancyResume(2)), editor.draft.value.resumes)
        onNodeWithTag("vacancy.resumes.select").performClick().performTextReplacement("Missing")
        onNodeWithTag("vacancy.resumes.option.1").assertDoesNotExist()
        onNodeWithTag("vacancy.resumes.option.2").assertDoesNotExist()
    }

    @Test
    fun escapeCancelsUnlinkAndConfirmationRemovesOnlyTheLink() = runDesktopComposeUiTest {
        val editor = showResumes(listOf(VacancyResume(1, 80), VacancyResume(2)))
        onNodeWithTag("vacancy.resumes.unlink.1").performClick()
        onNodeWithTag("vacancy.resumes.select").assertIsNotEnabled()
        onNodeWithTag("vacancy.resumes.unlink.dialog").performKeyInput { pressKey(Key.Escape) }
        assertEquals(2, editor.draft.value.resumes.size)
        onNodeWithTag("vacancy.resumes.unlink.1").performClick()
        onNode(hasText("Unlink") and hasAnyAncestor(hasTestTag("vacancy.resumes.unlink.dialog")))
            .performClick()
        assertEquals(listOf(VacancyResume(2)), editor.draft.value.resumes)
        assertEquals(2, editor.resumes.state.value.available.size)
        onNodeWithTag("vacancy.resumes.select").performClick()
        onNodeWithTag("vacancy.resumes.option.1").assertExists()
    }

    private fun DesktopComposeUiTest.showResumes(
        links: List<VacancyResume> = emptyList(),
    ): TestVacancyEditorComponent {
        val editor = TestVacancyEditorComponent()
        editor.draft.value = editor.draft.value.copy(resumes = links)
        editor.resumes.state.value = VacancyResumesState(
            available = listOf(
                Resume(
                    id = 1,
                    projectId = 7,
                    name = "Mobile CV",
                    createdAt = 0,
                    updatedAt = 0,
                    keywords = listOf("Kotlin"),
                ),
                Resume(id = 2, projectId = 7, name = "Backend CV", createdAt = 0, updatedAt = 0),
            ),
        )
        setContent {
            val draft by editor.draft.subscribeAsState()
            val state by editor.resumes.state.subscribeAsState()
            ReframeTheme {
                Column {
                    VacancyResumesContent(
                        draft.resumes,
                        state,
                        editor.resumes,
                        state.unlinkId == null,
                    )
                    VacancyResumeUnlinkConfirmation(state, editor.resumes)
                }
            }
        }
        return editor
    }
}
