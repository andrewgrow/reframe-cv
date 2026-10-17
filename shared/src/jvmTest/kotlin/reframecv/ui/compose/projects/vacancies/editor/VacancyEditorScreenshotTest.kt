package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import reframecv.attachments.SelectedAttachment
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.components.projects.vacancies.editor.VacancySnapshotState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyEditorScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun attachmentWithoutTitleMatchesReference() = runDesktopComposeUiTest {
        val component = TestVacancyEditorComponent()
        setGoldenContent(this) { VacancyEditorContent(component) }
        onNodeWithText("Attach source file").performScrollTo()
        runOnIdle {
            component.snapshot.state.value = VacancySnapshotState(
                selected = SelectedAttachment("unused", "job.pdf"),
            )
        }
        captureGolden(this, VACANCY_EDITOR_TAG)
    }

    @Test
    fun emptyPageMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { VacancyEditorContent(TestVacancyEditorComponent()) }
        onNodeWithText("Cancel").performClick()
        captureGolden(this, VACANCY_EDITOR_TAG)
    }

    @Test
    fun filledPageInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = ThemeMode.Light) {
            VacancyEditorContent(TestVacancyEditorComponent())
        }
        onNodeWithTag("vacancy.name").performTextReplacement("My Android shortlist")
        onNodeWithTag("vacancy.jobTitle").performTextReplacement("Senior Android Developer")
        onNodeWithTag("vacancy.company").performTextReplacement("Example")
        onNodeWithTag("vacancy.description").performTextReplacement("Build a Kotlin application.")
        onNodeWithTag(
            "vacancy.url",
        ).performScrollTo().performTextReplacement("https://example.com/job")
        onNodeWithTag(
            "vacancy.keywords",
        ).performScrollTo().performTextReplacement("Kotlin, Compose")
        onNodeWithText("Cancel").performClick()
        captureGolden(this, VACANCY_EDITOR_TAG)
    }

    @Test
    fun failedSaveMatchesReference() = runDesktopComposeUiTest {
        val component = TestVacancyEditorComponent()
        setGoldenContent(this) { VacancyEditorContent(component) }
        onNodeWithTag("vacancy.name").performTextReplacement("Backend Developer")
        runOnIdle { component.saveState.value = VacancySaveState.Failed }
        onNodeWithText("Could not save the vacancy. Please try again.").performScrollTo()
        onNodeWithText("Cancel").performClick()
        captureGolden(this, VACANCY_EDITOR_TAG)
    }
}
