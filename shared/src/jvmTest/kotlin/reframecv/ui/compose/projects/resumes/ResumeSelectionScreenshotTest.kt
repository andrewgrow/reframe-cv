package reframecv.ui.compose.projects.resumes

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.domain.models.resume.ResumeVacancyLink
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.ResumesState
import reframecv.ui.components.projects.resumes.TestResumesComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ResumeSelectionScreenshotTest : GoldenScreenshotTest() {
    private fun fixture() = TestResumesComponent(
        initialState = ResumesState.Ready(
            resumeSamples(),
            linkedVacancies = listOf(
                ResumeVacancyLink(resumeSamples()[0].id, 1, "ExampleCo — Senior Android Developer"),
                ResumeVacancyLink(resumeSamples()[0].id, 2, "ExampleCo — Mobile platform"),
            ),
        ),
    ).also { it.selection.onToggle(resumeSamples()[0].id) }

    @Test
    fun selectedResumesWithMultipleVacancyLinksMatchReference() = runComposeUiTest {
        setAndCaptureGolden(this) { ResumesContent(fixture()) }
    }

    @Test
    fun narrowListWithLinkedVacanciesMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(320.dp, 600.dp)) {
            ResumesContent(fixture())
        }
    }

    @Test
    fun bulkDeletionConfirmationInLightThemeMatchesReference() = runComposeUiTest {
        val component = fixture()
        component.selection.onToggleAll()
        component.selection.onRequestDelete()
        setAndCaptureGolden(
            this,
            themeMode = ReframeTheme.ThemeMode.Light,
            captureTag = "resumes.delete.dialog",
        ) {
            ResumesContent(component)
        }
    }
}
