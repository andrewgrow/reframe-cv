package reframecv.ui.compose.common.ai

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.components.common.ai.TestAiRequestDialogComponent
import reframecv.ui.compose.common.AiRequestDialogContent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class AiRequestDialogContentTest {
    @Test
    fun selectsConnectionsAndPromptsEditsTextAndKeepsDialogOpenAfterFailure() = runComposeUiTest {
        val connections = listOf("First", "Second").mapIndexed { index, name ->
            ProviderConnection(
                id = index.toLong() + 1,
                type = ApiProviderType.OpenRouter,
                name = name,
                apiKey = "test",
                modelId = "model-$index",
            )
        }
        val prompts = listOf(
            Prompt(id = 1, title = "Extract", text = "Extract original"),
            Prompt(id = 2, title = "Translate", text = "Translate to English"),
        )
        val component = TestAiRequestDialogComponent(
            AiRequestDialogState(
                "job.pdf",
                connections,
                prompts,
                connectionId = 1,
                promptId = 1,
                text = prompts.first().text,
                loading = false,
            ),
        )
        setContent {
            ReframeTheme {
                AiRequestDialogContent(component, "Parse PDF", "Replaces posting fields.")
            }
        }
        onNodeWithTag("ai.request.provider").performClick()
        onNodeWithTag("ai.request.provider.2").performClick()
        onNodeWithText("Model: model-1").assertExists()
        onNodeWithTag("ai.request.prompt").performClick()
        onNodeWithTag("ai.request.prompt.2").performClick()
        onNodeWithTag(
            "ai.request.text",
        ).performScrollTo().performTextReplacement("One-off instructions")
        onNodeWithTag("ai.request.savePrompt").performScrollTo().assertIsEnabled().performClick()
        onNodeWithText("Prompt successfully saved.").performScrollTo().assertExists()
        onNodeWithTag("ai.request.run").assertIsEnabled().performClick()
        onNodeWithTag("ai.request.error").assertExists()
        onNodeWithTag("ai.request.dialog").assertExists()
        runOnIdle {
            assertEquals(2L, component.state.value.connectionId)
            assertEquals(2L, component.state.value.promptId)
            assertEquals("One-off instructions", component.state.value.text)
        }
    }
}
