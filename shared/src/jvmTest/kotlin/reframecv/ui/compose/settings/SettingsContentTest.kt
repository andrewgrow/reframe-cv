package reframecv.ui.compose.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_close
import reframecv.shared.generated.resources.settings_key_hide
import reframecv.shared.generated.resources.settings_key_show
import reframecv.shared.generated.resources.settings_saved
import reframecv.testing.ComponentTest
import reframecv.testing.getTestString
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.TestProviderEditorComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.threading.runOnUiThread

@OptIn(ExperimentalTestApi::class)
class SettingsContentTest : ComponentTest() {
    @Test
    fun savesAnUnnamedConnectionAndStaysInTheSameEditor() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val root = runOnUiThread {
                lifecycle.resume()
                DefaultRootComponent(appComponentContext())
            }
            setContent { ReframeTheme { RootContent(root) } }
            onNodeWithTag("navigation.settings").performClick()
            onNodeWithTag("settings.add").performClick()
            onNodeWithTag("settings.key").performTextInput("example-key")
            onNodeWithTag("settings.save").performClick()
            waitForIdle()
            onNodeWithTag("settings.editor").assertExists()
            onNodeWithText(getTestString(Res.string.settings_saved)).assertExists()
            onNodeWithTag("settings.name").performTextInput("Offline connection")
            onNodeWithTag("settings.save").performClick()
            waitForIdle()
            onNodeWithText(getTestString(Res.string.settings_close)).performClick()
            onNodeWithText("Offline connection").assertExists()
            onNodeWithTag("settings.status.switch").performClick()
            waitForIdle()
            onNodeWithTag("settings.editor").assertDoesNotExist()
            onNodeWithTag("settings.status.switch").assertIsOff()
            onNodeWithTag("settings.connection.1").performClick()
            onNodeWithTag("settings.status.switch").assertIsOff()
        }

    @Test
    fun saveFeedbackDisappearsWithoutMovingTheControls() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            mainClock.autoAdvance = false
            val state = ProviderEditorState(
                ProviderConnection(
                    type = ApiProviderType.OpenRouter,
                    name = "",
                    apiKey = "key",
                    modelId = "",
                ),
                successfulSaves = 1,
            )
            setContent {
                ReframeTheme { ProviderEditorContent(TestProviderEditorComponent(state)) }
            }
            mainClock.advanceTimeByFrame()
            val message = getTestString(Res.string.settings_saved)
            onNodeWithText(message).assertExists()
            val before = onNodeWithTag("settings.save").fetchSemanticsNode().boundsInRoot
            mainClock.advanceTimeBy(3_100)
            onNodeWithText(message).assertDoesNotExist()
            assertEquals(before, onNodeWithTag("settings.save").fetchSemanticsNode().boundsInRoot)
        }

    @Test
    fun saveIsAvailableDuringModelDiscovery() = runDesktopComposeUiTest(width = 800, height = 600) {
        val state = ProviderEditorState(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Work",
                apiKey = "key",
                modelId = "",
            ),
            loading = true,
        )
        setContent { ReframeTheme { ProviderEditorContent(TestProviderEditorComponent(state)) } }
        onNodeWithTag("settings.save").assertIsEnabled()
    }

    @Test
    fun eyeTogglesKeyVisibilityWithoutChangingItAndNewEditorsStartMasked() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val connection = ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Personal",
                apiKey = "example-key",
                modelId = "model",
            )
            val component =
                mutableStateOf(TestProviderEditorComponent(ProviderEditorState(connection)))
            setContent { ReframeTheme { ProviderEditorContent(component.value) } }
            val key = onNodeWithTag("settings.key")
            fun displayedKey(): String {
                val results = mutableListOf<TextLayoutResult>()
                key.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
                return results.single().layoutInput.text.text
            }
            val masked = "•".repeat(connection.apiKey.length)
            assertEquals(masked, displayedKey())
            onNodeWithContentDescription(getTestString(Res.string.settings_key_show)).performClick()
            waitForIdle()
            onNodeWithContentDescription(getTestString(Res.string.settings_key_hide)).assertExists()
            assertEquals(connection.apiKey, displayedKey())
            onNodeWithContentDescription(getTestString(Res.string.settings_key_hide)).performClick()
            waitForIdle()
            assertEquals(masked, displayedKey())
            onNodeWithContentDescription(getTestString(Res.string.settings_key_show)).performClick()
            runOnIdle {
                assertEquals(connection, component.value.state.value.connection)
                component.value = TestProviderEditorComponent(ProviderEditorState(connection))
            }
            waitForIdle()
            assertEquals(masked, displayedKey())
        }

    @Test
    fun providerPickerOpensAndSelectsOpenRouter() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val state =
                ProviderEditorState(
                    ProviderConnection(
                        type = ApiProviderType.OpenRouter,
                        name = "",
                        apiKey = "",
                        modelId = "",
                    ),
                )
            setContent {
                ReframeTheme { ProviderEditorContent(TestProviderEditorComponent(state)) }
            }
            onNodeWithTag("settings.provider.select").performClick()
            onNodeWithTag("settings.provider.openrouter").assertExists().performClick()
            onNodeWithTag("settings.provider.openrouter").assertDoesNotExist()
            onNodeWithTag("settings.provider.select").assertExists()
        }

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
                ),
            )
            setContent { ReframeTheme { ProviderEditorContent(component) } }
            onNodeWithTag("settings.models.select").performClick()
            onNodeWithTag("settings.models.select").performTextInput("Beta")
            onNodeWithText("Beta model").performClick()
            runOnUiThread { assertEquals("b", component.state.value.connection.modelId) }
        }
}
