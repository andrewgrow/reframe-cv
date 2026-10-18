package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    ResizableDialog(
        component::onClose,
        modifier = Modifier.testTag("ai.request.dialog"),
        title = { Text(stringResource(Res.string.ai_request_title)) },
        text = {
            FastScrollColumn(
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                SelectableText(stringResource(Res.string.ai_request_source, state.sourceName))
                SelectableText(warning, color = ReframeTheme.colorScheme.hint)
                AiRequestChoices(component, state)
                AiRequestPrompt(component, state)
            }
        },
        actions = {
            AiRequestActions(component, state, action)
        },
    )
}
