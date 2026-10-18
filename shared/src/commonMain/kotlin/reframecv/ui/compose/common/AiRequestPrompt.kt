package reframecv.ui.compose.common

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.ai_request_prompt_saved
import reframecv.shared.generated.resources.ai_request_save_prompt
import reframecv.shared.generated.resources.prompts_resize_text
import reframecv.shared.generated.resources.prompts_text
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun AiRequestPrompt(
    component: AiRequestDialogComponent,
    state: AiRequestDialogState,
    scroll: ScrollState,
    viewport: Rect,
) {
    ResizableTextField(
        state.text,
        component::onText,
        enabled = !state.running && !state.savingPrompt,
        scroll = scroll,
        viewport = viewport,
        label = stringResource(Res.string.prompts_text),
        resizeLabel = stringResource(Res.string.prompts_resize_text),
        tag = "ai.request.text",
        minimumHeight = ReframeTheme.tokens.dimensions.promptTextMinHeight,
    )
    TextButton(
        component::onSavePrompt,
        enabled = !state.running && !state.savingPrompt && state.prompt != null &&
            state.text.isNotBlank() && state.text != state.prompt?.text,
        modifier = Modifier.testTag("ai.request.savePrompt"),
    ) { Text(stringResource(Res.string.ai_request_save_prompt)) }
    if (state.promptSaved) {
        SelectableText(
            stringResource(Res.string.ai_request_prompt_saved),
            color = ReframeTheme.colorScheme.success,
        )
    }
}
