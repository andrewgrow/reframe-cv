package reframecv.ui.compose.projects.resumes

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.domain.models.resume.ResumeVacancyLink
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.ResumesState
import reframecv.ui.components.projects.resumes.TestResumesComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeVacancyLinksScreenshotTest : GoldenScreenshotTest() {
    private val resume = resumeSamples().first()
    private val links = List(100) {
        ResumeVacancyLink(resume.id, it + 1L, "ExampleCo — Mobile Developer ${it + 1}")
    }

    @Test
    fun hundredLinkedVacanciesKeepTheRowCompact() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) {
            ResumesContent(
                TestResumesComponent(
                    initialState = ResumesState.Ready(listOf(resume), linkedVacancies = links),
                ),
            )
        }
    }

    @Test
    fun narrowRowWithOverflowMatchesReference() =
        runDesktopComposeUiTest(ScreenSize(320.dp, 600.dp)) {
            setAndCaptureGolden(this, screenSize = ScreenSize(320.dp, 600.dp)) {
                ResumesContent(
                    TestResumesComponent(
                        initialState = ResumesState.Ready(listOf(resume), linkedVacancies = links),
                    ),
                )
            }
        }

    @Test
    fun fullVacancyListInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(
            this,
            themeMode = ThemeMode.Light,
            captureTag = "resumes.links.dialog",
        ) {
            ResumeVacanciesDialog(ResumeRowLinks(links) {}, resume.name) {}
        }
    }
}
