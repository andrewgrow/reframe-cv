package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_improvement_warning
import reframecv.shared.generated.resources.vacancy_resume_improve
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.components.common.ai.TestAiRequestDialogComponent
import reframecv.ui.compose.common.AiRequestDialogContent

@OptIn(ExperimentalTestApi::class)
class ResumeImprovementScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun improvementRequestShowsDocumentsAndImmediateSaveWarning() = runDesktopComposeUiTest {
        val provider =
            ProviderConnection(
                id = 1,
                type = ApiProviderType.OpenRouter,
                name = "Work",
                configuration = OpenRouterConfiguration(
                    apiKey = "key",
                    modelId = "example/model",
                ),
            )
        val prompt =
            Prompt(
                id = 1,
                title = "Improve resume",
                text = "Adapt resume.json to vacancy.json. Preserve facts. " +
                    "Return a complete resume JSON.",
            )
        val component = TestAiRequestDialogComponent(
            AiRequestDialogState(
                "main resume (resume.json) + Android vacancy (vacancy.json)",
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
                stringResource(Res.string.vacancy_resume_improve),
                stringResource(Res.string.resume_improvement_warning),
            )
        }
    }
}
