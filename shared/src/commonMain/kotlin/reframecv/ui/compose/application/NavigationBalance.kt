package reframecv.ui.compose.application

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.ProviderCreditBalance
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_remaining_balance
import reframecv.ui.compose.settings.providerAmount
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun NavigationBalance(balance: ProviderCreditBalance?, collapsed: Boolean) {
    val tokens = ReframeTheme.tokens
    if (balance != null) {
        Column(
            Modifier.fillMaxWidth().padding(tokens.spacing.small).testTag("navigation.balance"),
        ) {
            if (!collapsed) {
                Text(
                    stringResource(Res.string.navigation_remaining_balance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "$" + providerAmount(balance.remainingUsd).replace('.', ','),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
