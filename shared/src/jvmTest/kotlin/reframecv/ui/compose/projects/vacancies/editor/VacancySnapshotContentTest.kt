package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.attachments.SelectedAttachment
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySnapshotState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancySnapshotContentTest {
    @Test
    fun selectingAttachmentHighlightsMissingTitleAndTypingClearsError() = runComposeUiTest {
        val component = TestVacancyEditorComponent()
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithTag("vacancy.name").assert(
            SemanticsMatcher.keyNotDefined(SemanticsProperties.Error),
        )
        onNodeWithText("Attach source file").performScrollTo()
        runOnIdle {
            component.snapshot.state.value = VacancySnapshotState(
                selected = SelectedAttachment("unused", "job.pdf"),
            )
        }
        onNodeWithTag("vacancy.name").assertIsFocused().assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Error),
        )
        onNodeWithText("Enter a name to save the vacancy.").assertExists()
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithTag("vacancy.name").performTextReplacement("   ")
        onNodeWithText("Enter a name to save the vacancy.").assertExists()
        onNodeWithTag("vacancy.name").performTextReplacement("Android Developer")
        onNodeWithText("Enter a name to save the vacancy.").assertDoesNotExist()
        onNodeWithTag("vacancy.name").assert(
            SemanticsMatcher.keyNotDefined(SemanticsProperties.Error),
        )
        onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun selectingAttachmentWithValidTitleDoesNotShowError() = runComposeUiTest {
        val component = TestVacancyEditorComponent()
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithTag("vacancy.name").performTextReplacement("Backend")
        runOnIdle {
            component.snapshot.state.value = VacancySnapshotState(
                selected = SelectedAttachment("unused", "job.pdf"),
            )
        }
        onNodeWithText("Enter a name to save the vacancy.").assertDoesNotExist()
        onNodeWithTag("vacancy.name").assert(
            SemanticsMatcher.keyNotDefined(SemanticsProperties.Error),
        )
        onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun creationShowsAttachmentAction() = runComposeUiTest {
        setContent { ReframeTheme { VacancyEditorContent(TestVacancyEditorComponent()) } }
        onNodeWithText("Attach source file").performScrollTo().assertIsEnabled()
        onNodeWithText(
            "Attach an original snapshot as JSON, HTML, TXT, PNG, JPG, PDF, or DOCX. A copy is saved with the vacancy.",
        ).performScrollTo().assertExists()
    }

    @Test
    fun editingShowsAttachmentAction() = runComposeUiTest {
        val vacancy =
            Vacancy(id = 1, projectId = 7, name = "Backend", createdAt = 100, updatedAt = 200)
        setContent {
            ReframeTheme {
                VacancyEditorContent(TestVacancyEditorComponent(initialVacancy = vacancy))
            }
        }
        onNodeWithText("Attach source file").performScrollTo().assertIsEnabled()
        onNodeWithText(
            "Attach an original snapshot as JSON, HTML, TXT, PNG, JPG, PDF, or DOCX. A copy is saved with the vacancy.",
        ).performScrollTo().assertExists()
    }
}
