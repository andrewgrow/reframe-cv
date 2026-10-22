package reframecv.ui.compose.common.ai

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.components.common.ai.TestAiRequestDialogComponent
import reframecv.ui.compose.common.AiRequestDialogContent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class AiRequestDialogScreenshotTest : GoldenScreenshotTest() {
    private val selected = AiRequestDialogState(
        sourceName = "Mobile Developer.pdf",
        connections = listOf(
            ProviderConnection(
                id = 1,
                type = ApiProviderType.OpenRouter,
                name = "My OpenRouter",
                configuration = OpenRouterConfiguration(
                    apiKey = "test",
                    modelId = "anthropic/claude-sonnet",
                ),
            ),
        ),
        prompts = listOf(
            Prompt(
                id = 1,
                title = "Extract vacancy in English",
                text = "Extract the job posting. Return all extracted content in English.",
            ),
        ),
        connectionId = 1,
        promptId = 1,
        text = "Extract the job posting. Return all extracted content in English.",
        loading = false,
    )

    @Test fun selectedRequestMatchesReference() = capture(selected)

    @Test fun emptyChoicesInLightThemeMatchReference() =
        capture(AiRequestDialogState("job.pdf", loading = false), ThemeMode.Light)

    @Test fun failedRequestKeepsChoicesMatchesReference() =
        capture(selected.copy(error = "No identifiable job posting was found in the source."))

    @Test fun processingRequestMatchesReference() = capture(selected.copy(running = true))

    private fun capture(state: AiRequestDialogState, theme: ThemeMode = ThemeMode.Dark) =
        runDesktopComposeUiTest {
            setAndCaptureGolden(this, captureTag = "ai.request.dialog", themeMode = theme) {
                AiRequestDialogContent(
                    TestAiRequestDialogComponent(state),
                    "Parse PDF",
                    "The result will replace vacancy fields except Internal name, " +
                        "Keywords, and the attachment. " +
                        "Save changes on the vacancy page.",
                )
            }
        }
}
