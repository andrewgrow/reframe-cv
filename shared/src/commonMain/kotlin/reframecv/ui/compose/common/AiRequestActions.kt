package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.ai_request_cancel
import reframecv.shared.generated.resources.navigation_retry
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun AiRequestActions(
    component: AiRequestDialogComponent,
    state: AiRequestDialogState,
    action: String,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        if (state.running || state.savingPrompt || state.loading) {
            CircularProgressIndicator(Modifier.testTag("ai.request.progress"))
        }
        state.error?.let {
            SelectableText(
                it,
                color = ReframeTheme.colorScheme.critical,
                modifier = Modifier.testTag("ai.request.error"),
            )
            if (state.choicesFailed) {
                TextButton(component::onRetry) {
                    Text(stringResource(Res.string.navigation_retry))
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        ) {
            TextButton(component::onClose) {
                Text(stringResource(Res.string.ai_request_cancel))
            }
            TextButton(
                component::onRun,
                enabled = state.canRun,
                modifier = Modifier.testTag("ai.request.run"),
            ) { Text(action) }
        }
    }
}
