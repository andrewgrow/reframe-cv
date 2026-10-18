package reframecv.ui.compose.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.ComponentTest
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.TestProviderEditorComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.threading.runOnUiThread

@OptIn(ExperimentalTestApi::class)
class SettingsContentTest : ComponentTest() {
    @Test
    fun opensFromTheFooterAndEscapeClosesTheEditorBeforeLeavingSettings() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val root = runOnUiThread {
                lifecycle.resume()
                DefaultRootComponent(appComponentContext())
            }
            setContent { ReframeTheme { RootContent(root) } }
            onNodeWithTag("navigation.settings").performClick()
            onNodeWithTag("settings.page").assertExists()
            onNodeWithTag("settings.add").performClick()
            onNodeWithTag("settings.editor").assertExists()
            onNodeWithTag("settings.save").performClick()
            onNodeWithTag("settings.error").assertExists()
            onNodeWithTag("settings.name").performTextInput("Personal")
            onNodeWithTag("settings.key").performTextInput("example-key")
            onRoot().performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("settings.editor").assertDoesNotExist()
            onNodeWithTag("settings.page").assertExists()
            onRoot().performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("settings.page").assertDoesNotExist()
        }

    @Test
    fun modelPickerSearchSelectsAProviderModel() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val component = TestProviderEditorComponent(
                ProviderEditorState(
                    ProviderConnection(
                        type = ApiProviderType.OpenRouter,
                        name = "Work",
                        apiKey = "key",
                        modelId = "a",
                    ),
                    listOf(AiModel("a", "Alpha model"), AiModel("b", "Beta model")),
                    verified = true,
                ),
            )
            setContent { ReframeTheme { ProviderEditorContent(component) } }
            onNodeWithTag("settings.models.select").performClick()
            onNodeWithTag("settings.models.search").performTextInput("Beta")
            onNodeWithText("Beta model").performClick()
            runOnUiThread { assertEquals("b", component.state.value.connection.modelId) }
        }
}
