package reframecv.ui.compose.common

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_create
import reframecv.shared.generated.resources.resume_name
import reframecv.shared.generated.resources.resume_name_hint
import reframecv.shared.generated.resources.resume_update
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState

@OptIn(ExperimentalTestApi::class)
class ConfirmationDialogTest : GoldenScreenshotTest() {
    private fun content() = ConfirmationDialogContent(
        title = Res.string.resume_create,
        message = Res.string.resume_name_hint,
        confirmLabel = Res.string.resume_update,
        cancelLabel = Res.string.resume_cancel,
        inputLabel = Res.string.resume_name,
    )

    @Test
    fun reportsConfirmCancelAndBackWithCallerControlledValidation() = runDesktopComposeUiTest {
        val input = mutableStateOf("")
        val visible = mutableStateOf(true)
        val actions = mutableListOf<ConfirmationDialogAction>()
        setGoldenContent(this) {
            if (visible.value) {
                ConfirmationDialog(
                    content(),
                    ConfirmationDialogState(input.value, confirmEnabled = input.value == "READY"),
                    onInputChange = { input.value = it },
                    onAction = {
                        actions += it
                        if (it != ConfirmationDialogAction.Confirm) visible.value = false
                    },
                )
            }
        }
        onNodeWithText("Update").assertIsNotEnabled()
        onNodeWithTag("confirmation.input").performTextReplacement("READY")
        onNodeWithText("Update").assertIsEnabled()
        onNodeWithTag("confirmation.input").performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(ConfirmationDialogAction.Confirm), actions)
        onNodeWithText("Cancel").performClick()
        onNodeWithTag("confirmation.dialog").assertDoesNotExist()
        runOnIdle { visible.value = true }
        onNodeWithTag("confirmation.input").performClick().performKeyInput { pressKey(Key.Escape) }
        assertEquals(
            listOf(
                ConfirmationDialogAction.Confirm,
                ConfirmationDialogAction.Cancel,
                ConfirmationDialogAction.Back,
            ),
            actions,
        )
    }

    @Test
    fun busyDialogBlocksInputConfirmCancelAndBack() = runDesktopComposeUiTest {
        val busy = mutableStateOf(true)
        val actions = mutableListOf<ConfirmationDialogAction>()
        setGoldenContent(this) {
            ConfirmationDialog(
                content(),
                ConfirmationDialogState(confirmEnabled = true, busy = busy.value),
                actions::add,
            )
        }
        onNodeWithText("Update").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithTag("confirmation.input").assertIsNotEnabled()
        onNodeWithTag("confirmation.input").performKeyInput { pressKey(Key.Escape) }
        assertEquals(emptyList(), actions)
        runOnIdle { busy.value = false }
        onNodeWithText("Update").performClick()
        assertEquals(listOf(ConfirmationDialogAction.Confirm), actions)
    }

    @Test
    fun supportsConfirmationWithoutInput() = runDesktopComposeUiTest {
        val actions = mutableListOf<ConfirmationDialogAction>()
        setGoldenContent(this) {
            ConfirmationDialog(
                content().copy(inputLabel = null),
                ConfirmationDialogState(),
                actions::add,
            )
        }
        onNodeWithTag("confirmation.input").assertDoesNotExist()
        onNodeWithText("Update").performClick()
        assertEquals(listOf(ConfirmationDialogAction.Confirm), actions)
    }
}
