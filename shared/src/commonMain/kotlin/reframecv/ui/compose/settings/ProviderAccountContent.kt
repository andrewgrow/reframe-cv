package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderBudget
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_account_empty
import reframecv.shared.generated.resources.settings_account_error
import reframecv.shared.generated.resources.settings_account_expires
import reframecv.shared.generated.resources.settings_account_no_expiry
import reframecv.shared.generated.resources.settings_account_refresh
import reframecv.shared.generated.resources.settings_account_refreshing
import reframecv.shared.generated.resources.settings_account_updated
import reframecv.shared.generated.resources.settings_budget_effective
import reframecv.shared.generated.resources.settings_budget_key
import reframecv.shared.generated.resources.settings_budget_limit
import reframecv.shared.generated.resources.settings_budget_no_reset
import reframecv.shared.generated.resources.settings_budget_other
import reframecv.shared.generated.resources.settings_budget_remaining
import reframecv.shared.generated.resources.settings_budget_reset
import reframecv.shared.generated.resources.settings_budget_spent
import reframecv.shared.generated.resources.settings_budget_unknown
import reframecv.shared.generated.resources.settings_key_info
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderAccountContent(state: ProviderEditorState, onRefresh: () -> Unit) {
    val spacing = ReframeTheme.tokens.spacing
    val info = state.connection.openRouterCache.accountInfo
    Surface(
        Modifier.fillMaxWidth().testTag("settings.account"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            HorizontalDivider(Modifier.padding(vertical = spacing.small))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(Res.string.settings_key_info),
                    style = MaterialTheme.typography.titleMedium,
                )
                FillButton(
                    stringResource(Res.string.settings_account_refresh),
                    onRefresh,
                    Modifier.testTag("settings.account.refresh"),
                    enabled =
                        state.connection.active && !state.busy && !state.refreshingAccount &&
                            state.connection.openRouter.apiKey.isNotBlank(),
                    outlineState = FillButton.OutlineState.Visible,
                )
            }
            if (state.refreshingAccount) {
                Text(
                    stringResource(Res.string.settings_account_refreshing),
                )
            }
            if (state.accountError != null) {
                Text(
                    providerErrorMessage(
                        stringResource(Res.string.settings_account_error),
                        state.accountErrorDetails,
                    ),
                    color = ReframeTheme.colorScheme.critical,
                    modifier = Modifier.testTag("settings.account.error"),
                )
            }
            if (info == null) {
                Text(
                    stringResource(Res.string.settings_account_empty),
                    color = ReframeTheme.colorScheme.hint,
                )
            } else {
                AccountSnapshotContent(info)
            }
        }
    }
}

@Composable
private fun AccountSnapshotContent(info: ProviderAccountInfo) {
    Text(
        stringResource(Res.string.settings_account_updated, accountTimestamp(info.fetchedAt)),
        color = ReframeTheme.colorScheme.hint,
    )
    Text(
        stringResource(
            Res.string.settings_account_expires,
            info.expiresAt?.let(::accountTimestamp)
                ?: stringResource(Res.string.settings_account_no_expiry),
        ),
    )
    info.effectiveBudget?.let { BudgetContent(it, effective = true) }
    info.budgets.filter { it != info.effectiveBudget }.forEach { BudgetContent(it) }
}

@Composable
private fun BudgetContent(budget: ProviderBudget, effective: Boolean = false) {
    val title = when {
        effective -> stringResource(Res.string.settings_budget_effective, budget.scope)
        budget.scope == "key_limit" -> stringResource(Res.string.settings_budget_key)
        else -> stringResource(Res.string.settings_budget_other, budget.scope)
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.settings_budget_limit, usd(budget.limitUsd)))
            Text(stringResource(Res.string.settings_budget_spent, usd(budget.spendUsd)))
            Text(stringResource(Res.string.settings_budget_remaining, usd(budget.remainingUsd)))
            Text(
                stringResource(
                    Res.string.settings_budget_reset,
                    budget.resetsAt?.let(::accountTimestamp)
                        ?: stringResource(Res.string.settings_budget_no_reset),
                ),
            )
        }
    }
}

@Composable
private fun usd(value: Double?): String = value?.let { "${providerAmount(it)} USD" }
    ?: stringResource(Res.string.settings_budget_unknown)

internal fun accountTimestamp(value: Long): String {
    val utc = Instant.fromEpochMilliseconds(value).toLocalDateTime(TimeZone.UTC)
    return "${utc.date} ${utc.hour.toString().padStart(
        2,
        '0',
    )}:${utc.minute.toString().padStart(2, '0')} UTC"
}
