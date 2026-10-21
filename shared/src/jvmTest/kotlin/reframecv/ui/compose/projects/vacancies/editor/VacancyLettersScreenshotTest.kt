package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyLettersState
import reframecv.ui.components.projects.vacancies.editor.VacancyResumesState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyLettersScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun linkedLettersWithCollapsedResumesMatchReference() = runDesktopComposeUiTest {
        val component = fixture()
        setAndCaptureGolden(this) {
            Column(Modifier.padding(ReframeTheme.tokens.spacing.medium)) {
                VacancyDocumentsContent(component, true)
            }
        }
    }

    @Test
    fun unlinkConfirmationInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = fixture()
        component.letters.onUnlink(1)
        setAndCaptureGolden(
            this,
            themeMode = ReframeTheme.ThemeMode.Light,
            captureTag = "vacancy.letters.unlink.dialog",
        ) {
            VacancyLetterDialogs(component)
        }
    }

    @Test
    fun emptyLetterBlockWithoutResumeMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) {
            Column(Modifier.padding(ReframeTheme.tokens.spacing.medium)) {
                VacancyLettersContent(TestVacancyEditorComponent(), true)
            }
        }
    }

    @Test
    fun resumeSelectionActionsMatchReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = "vacancy.letters.source.dialog") {
            VacancyLetterDialogs(letterResumeSelectionFixture())
        }
    }

    @Test
    fun resumeSelectionInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(
            this,
            themeMode = ReframeTheme.ThemeMode.Light,
            captureTag = "vacancy.letters.source.dialog",
        ) { VacancyLetterDialogs(letterResumeSelectionFixture()) }
    }

    private fun fixture() = TestVacancyEditorComponent().also {
        it.resumeSection.onToggle()
        it.draft.value =
            it.draft.value.copy(coverLetterIds = listOf(1, 2), resumes = listOf(VacancyResume(1)))
        it.resumes.state.value =
            VacancyResumesState(available = listOf(Resume(1, 7, "Tailored resume", 0, 0)))
        it.letters.state.value =
            VacancyLettersState(
                available = listOf(
                    CoverLetter(1, 7, "ExampleCo — Android application", 0, 0),
                    CoverLetter(2, 7, "Short version", 0, 0),
                ),
            )
    }
}
