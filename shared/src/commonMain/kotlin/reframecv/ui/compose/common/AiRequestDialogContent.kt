package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.ai_request_source
import reframecv.shared.generated.resources.ai_request_title
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.theme.ReframeTheme

/** Presents a source and editable AI request without knowing how its result will be applied. */
@Composable
internal fun AiRequestDialogContent(
    component: AiRequestDialogComponent,
    action: String,
    warning: String,
) {
    val state by component.state.subscribeAsState()
    val scroll = rememberScrollState()
    var viewport by remember { mutableStateOf(Rect.Zero) }
    ResizableDialog(
        component::onClose,
        modifier = Modifier.testTag("ai.request.dialog"),
        title = { Text(stringResource(Res.string.ai_request_title)) },
        text = {
            FastScrollColumn(
                scrollState = scroll,
                contentModifier = Modifier.onGloballyPositioned { viewport = it.boundsInWindow() },
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                SelectableText(stringResource(Res.string.ai_request_source, state.sourceName))
                SelectableText(warning, color = ReframeTheme.colorScheme.hint)
                AiRequestChoices(component, state)
                AiRequestPrompt(component, state, scroll, viewport)
            }
        },
        actions = {
            AiRequestActions(component, state, action)
        },
    )
}
