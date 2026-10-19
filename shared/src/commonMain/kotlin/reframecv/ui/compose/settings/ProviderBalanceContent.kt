package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.ProviderCreditBalance
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_account_updated
import reframecv.shared.generated.resources.settings_balance_empty
import reframecv.shared.generated.resources.settings_balance_error
import reframecv.shared.generated.resources.settings_balance_loading
import reframecv.shared.generated.resources.settings_balance_missing
import reframecv.shared.generated.resources.settings_balance_remaining
import reframecv.shared.generated.resources.settings_balance_title
import reframecv.shared.generated.resources.settings_balance_total
import reframecv.shared.generated.resources.settings_balance_used
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderBalanceContent(state: ProviderEditorState) {
    val balance = state.connection.creditBalance
    val tokens = ReframeTheme.tokens
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("settings.balance"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.padding(tokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            Text(
                stringResource(Res.string.settings_balance_title),
                style = MaterialTheme.typography.titleSmall,
            )
            when {
                state.connection.managementApiKey.isBlank() -> Text(
                    stringResource(Res.string.settings_balance_missing),
                )

                state.refreshingBalance -> Text(stringResource(Res.string.settings_balance_loading))

                balance == null && !state.balanceError -> Text(
                    stringResource(Res.string.settings_balance_empty),
                )
            }
            if (state.balanceError) {
                Text(
                    providerErrorMessage(
                        stringResource(Res.string.settings_balance_error),
                        state.balanceErrorDetails,
                    ),
                    color = ReframeTheme.colorScheme.critical,
                    modifier = Modifier.testTag("settings.balance.error"),
                )
            }
            balance?.let { BalanceAmounts(it) }
        }
    }
}

@Composable
private fun BalanceAmounts(balance: ProviderCreditBalance) {
    Text(
        stringResource(Res.string.settings_balance_remaining, providerAmount(balance.remainingUsd)),
    )
    Text(stringResource(Res.string.settings_balance_total, providerAmount(balance.totalCreditsUsd)))
    Text(stringResource(Res.string.settings_balance_used, providerAmount(balance.totalUsageUsd)))
    Text(
        stringResource(Res.string.settings_account_updated, accountTimestamp(balance.fetchedAt)),
        color = ReframeTheme.colorScheme.hint,
    )
}
