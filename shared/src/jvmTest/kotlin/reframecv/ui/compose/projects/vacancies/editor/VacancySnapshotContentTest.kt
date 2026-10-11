package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancySnapshotContentTest {
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
