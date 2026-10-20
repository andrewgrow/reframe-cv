package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.assessment.ResumeAssessment
import reframecv.assessment.ResumeAssessmentResult
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyResumesComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyResumesState
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode
import reframecv.ui.theme.UiScale

@OptIn(ExperimentalTestApi::class)
class VacancyResumesScreenshotTest : GoldenScreenshotTest() {
    private val resumes = listOf(
        Resume(
            id = 1,
            projectId = 7,
            name = "Senior Android Developer — Kotlin and Compose",
            createdAt = 0,
            updatedAt = 0,
        ),
        Resume(id = 2, projectId = 7, name = "Backend Developer", createdAt = 0, updatedAt = 0),
    )
    private val links = listOf(VacancyResume(1), VacancyResume(2, 82))
    private val perfectAssessment = ResumeAssessment(
        ResumeAssessmentResult(100, "Matches all requirements."),
        1,
        0,
        "{}",
        "Work",
        "model",
    )

    @Test
    fun perfectScoreMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) { ResumeLinks(listOf(VacancyResume(1, 100, perfectAssessment))) }
    }

    @Test
    fun perfectScoreAtIncreasedScaleMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) {
            ReframeTheme(themeMode = ThemeMode.Dark, uiScale = UiScale(150)) {
                ResumeLinks(listOf(VacancyResume(1, 100, perfectAssessment)))
            }
        }
    }

    @Test
    fun emptyPickerMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) { ResumeLinks(emptyList()) }
    }

    @Test
    fun linkedResumesMatchReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) { ResumeLinks(links) }
    }

    @Test
    fun linkedResumesInLightThemeMatchReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) { ResumeLinks(links) }
    }

    @Test
    fun unlinkConfirmationMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = "vacancy.resumes.unlink.dialog") {
            ResumeLinks(links, unlinkId = 1)
        }
    }

    @Composable
    private fun ResumeLinks(selected: List<VacancyResume>, unlinkId: Long? = null) {
        val state = VacancyResumesState(available = resumes, unlinkId = unlinkId)
        val component = TestVacancyResumesComponent(current = { selected }, changed = {})
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(ReframeTheme.tokens.spacing.medium),
        ) {
            VacancyResumesContent(selected, state, component, enabled = unlinkId == null)
            VacancyResumeUnlinkConfirmation(state, component)
        }
    }
}
