package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import kotlin.test.Test
import reframecv.attachments.SelectedAttachment
import reframecv.recognition.RecognitionError
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyRecognitionState
import reframecv.ui.components.projects.vacancies.editor.VacancySnapshotState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyRecognitionScreenshotTest : GoldenScreenshotTest() {
    @Test fun selectedPdfMatchesReference() = capture(VacancyRecognitionState.Idle)

    private fun capture(state: VacancyRecognitionState, theme: ThemeMode = ThemeMode.Dark) =
        runDesktopComposeUiTest {
            val component = TestVacancyEditorComponent()
            component.draft.value =
                VacancyDraft("My shortlist", jobTitle = "Mobile developer", company = "Example")
            component.snapshot.state.value =
                VacancySnapshotState(selected = SelectedAttachment("source", "job.pdf"))
            component.recognition.state.value = state
            setGoldenContent(this, themeMode = theme) { VacancyEditorContent(component) }
            val target = when (state) {
                VacancyRecognitionState.Processing -> "Parsing vacancy from PDF..."

                is VacancyRecognitionState.Failed ->
                    state.details
                        ?: "PDF parsing is not configured yet."

                else -> "Parse PDF"
            }
            onNodeWithText(target).performScrollTo()
            captureGolden(this, VACANCY_EDITOR_TAG)
        }
}
