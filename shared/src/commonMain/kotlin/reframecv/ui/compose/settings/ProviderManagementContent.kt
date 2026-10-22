package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_account_refresh
import reframecv.shared.generated.resources.settings_account_title
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

/** Groups management-only account data separately from ordinary model-key limits. */
@Composable
internal fun ProviderManagementContent(state: ProviderEditorState, onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(Res.string.settings_account_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                FillButton(
                    stringResource(Res.string.settings_account_refresh),
                    onRefresh,
                    Modifier.testTag("settings.balance.refresh"),
                    enabled = state.connection.active && !state.busy && !state.refreshingBalance &&
                        state.connection.openRouter.managementApiKey.isNotBlank(),
                    outlineState = FillButton.OutlineState.Visible,
                )
            }
            ProviderBalanceContent(state)
        }
    }
}
