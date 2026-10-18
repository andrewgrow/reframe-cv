package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.ProviderEditorState

@OptIn(ExperimentalTestApi::class)
class ProviderModelPickerTest : GoldenScreenshotTest() {
    private val connection = ProviderConnection(
        type = ApiProviderType.OpenRouter,
        name = "",
        apiKey = "key",
        modelId = "alpha",
    )

    @Test
    fun typingInThePickerMatchesPartsOfNamesAndIdsIgnoringCase() = runDesktopComposeUiTest {
        var selected = ""
        val models = listOf(
            AiModel("alpha", "Alpha model"),
            AiModel("anthropic/claude-sonnet", "Anthropic: Claude Sonnet", true),
            AiModel("perplexity/sonar", "Perplexity search"),
        )
        setGoldenContent(this) {
            ProviderModelPicker(ProviderEditorState(connection, models), {
                selected = it
            }, Modifier.width(560.dp))
        }
        onNodeWithTag("settings.models.select").performClick().performTextInput("SoN")
        onNodeWithTag("settings.models.option.alpha").assertDoesNotExist()
        onNodeWithTag("settings.models.option.anthropic/claude-sonnet").assertIsDisplayed()
        onNodeWithTag("settings.models.option.perplexity/sonar").assertIsDisplayed().performClick()
        assertEquals("perplexity/sonar", selected)
        onNodeWithTag("settings.models.menu").assertDoesNotExist()
    }

    @Test
    fun scrollControlsReachTheEndsAndFilteringResetsThePosition() = runDesktopComposeUiTest {
        val models = List(60) { AiModel("model-$it", "Model $it") }
        setGoldenContent(this) {
            ProviderModelPicker(ProviderEditorState(connection, models), {}, Modifier.width(560.dp))
        }
        onNodeWithTag("settings.models.select").performClick()
        onNodeWithTag("scroll.to.top").assertIsNotEnabled()
        onNodeWithTag("scroll.to.bottom").performClick()
        onNodeWithTag("settings.models.option.model-59").assertIsDisplayed()
        onNodeWithTag("scroll.to.top").performClick()
        onNodeWithTag("settings.models.option.model-0").assertIsDisplayed()
        onNodeWithTag("scroll.track").performTouchInput {
            swipe(Offset(center.x, 16f), Offset(center.x, height - 16f))
        }
        onNodeWithTag("settings.models.option.model-59").assertIsDisplayed()
        onNodeWithTag("settings.models.select").performTextInput("Model 0")
        onNodeWithTag("settings.models.option.model-0").assertIsDisplayed()
        onNodeWithTag("scroll.to.top").assertIsNotEnabled()
        onNodeWithTag("scroll.to.bottom").assertIsNotEnabled()
        onNodeWithTag("settings.models.select").performTextReplacement("no such model")
        onNodeWithTag("settings.models.option.model-0").assertDoesNotExist()
        onNodeWithTag("settings.models.menu").assertExists()
    }

    @Test
    fun spacedModelOptionsMatchReference() = runDesktopComposeUiTest {
        val models = listOf(
            AiModel("anthropic/claude-sonnet", "Anthropic: Claude Sonnet", true),
            AiModel("openai/example", "OpenAI: Example model"),
            AiModel("perplexity/sonar", "Perplexity: Sonar", true),
            AiModel("google/example", "Google: Example model"),
        )
        setGoldenContent(this) {
            ProviderModelPicker(
                ProviderEditorState(connection, models),
                {},
                Modifier.width(560.dp).padding(16.dp),
            )
        }
        onNodeWithTag("settings.models.select").performClick()
        captureGolden(this, "settings.models.menu")
    }
}
