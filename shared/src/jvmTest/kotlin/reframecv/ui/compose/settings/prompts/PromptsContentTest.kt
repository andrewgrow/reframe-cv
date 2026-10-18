package reframecv.ui.compose.settings.prompts

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import reframecv.domain.models.prompts.PromptComponent
import reframecv.testing.ComponentTest
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.threading.runOnUiThread

@OptIn(ExperimentalTestApi::class)
class PromptsContentTest : ComponentTest() {
    @Test
    fun createsEditsAndDisablesPromptsWithoutHidingThemFromSettings() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val root = runOnUiThread {
                lifecycle.resume()
                DefaultRootComponent(appComponentContext())
            }
            setContent { ReframeTheme { RootContent(root) } }
            onNodeWithTag("navigation.settings").performClick()
            onNodeWithTag("settings.prompts").performClick()
            onNodeWithText("No prompts yet.").assertExists()
            onNodeWithTag("prompts.add").performClick()
            onNodeWithTag("prompts.save").performClick()
            onNodeWithText("Enter prompt text before saving.").assertExists()
            onNodeWithTag("prompts.component").performScrollTo().performClick()
            onNodeWithTag("prompts.component.Resume").performClick()
            onNodeWithTag(
                "prompts.text",
            ).performScrollTo().performTextInput("Extract visible facts only.")
            onNodeWithTag("prompts.status").performScrollTo()
            onNodeWithTag("settings.status.switch").performClick()
            onNodeWithTag("prompts.save").performClick()
            waitForIdle()
            onNodeWithTag("prompts.editor").assertExists()
            val saved = dependencies.promptsRepository.records.value.last()
            assertFalse(saved.active)
            assertEquals(PromptComponent.Resume, saved.component)
            onRoot().performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("prompts.row.${saved.id}").assertExists().performClick()
            onNodeWithTag("prompts.title").performScrollTo().performTextInput("Unsaved title")
            onRoot().performKeyInput { pressKey(Key.Escape) }
            assertEquals("", dependencies.promptsRepository.records.value.last().title)
            onRoot().performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("settings.page").assertExists()
        }
}
