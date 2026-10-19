package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import org.jetbrains.compose.resources.stringResource
import reframecv.assessment.ResumeAssessment
import reframecv.assessment.ResumeAssessmentResult
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_assessment_warning
import reframecv.shared.generated.resources.vacancy_resume_assess
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.components.common.ai.TestAiRequestDialogComponent
import reframecv.ui.compose.common.AiRequestDialogContent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeAssessmentScreenshotTest : GoldenScreenshotTest() {
    private val assessment = ResumeAssessment(
        ResumeAssessmentResult(
            82,
            "Strong Kotlin and Android experience. Compose experience is documented, " +
                "but German proficiency is not evidenced in this resume.",
        ),
        1792432000000,
        5,
        "{}",
        "OpenRouter",
        "example/model",
    )

    @Test
    fun resultMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = "vacancy.assessment.result") {
            ResumeAssessmentDialog(assessment, stale = false, close = {})
        }
    }

    @Test
    fun staleResultInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(
            this,
            captureTag = "vacancy.assessment.result",
            themeMode = ThemeMode.Light,
        ) {
            ResumeAssessmentDialog(assessment, stale = true, close = {})
        }
    }

    @Test
    fun requestShowsSourcesAndSavingBehavior() = runDesktopComposeUiTest {
        val provider =
            ProviderConnection(
                id = 1,
                type = ApiProviderType.OpenRouter,
                name = "Work",
                apiKey = "key",
                modelId = "example/model",
            )
        val prompt =
            Prompt(
                id = 1,
                title = "Assess resume match",
                text = "Assess resume.json against vacancy.json. Return score and summary.",
            )
        val component = TestAiRequestDialogComponent(
            AiRequestDialogState(
                "Mobile CV (resume.json) + Android vacancy (vacancy.json)",
                connections = listOf(provider),
                prompts = listOf(prompt),
                connectionId = 1,
                promptId = 1,
                text = prompt.text,
                loading = false,
            ),
        )
        setAndCaptureGolden(this, captureTag = "ai.request.dialog") {
            AiRequestDialogContent(
                component,
                stringResource(Res.string.vacancy_resume_assess),
                stringResource(Res.string.resume_assessment_warning),
            )
        }
    }
}
