package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.runners_add
import reframecv.shared.generated.resources.runners_empty
import reframecv.shared.generated.resources.runners_load_error
import reframecv.shared.generated.resources.runners_save_connection
import reframecv.shared.generated.resources.runners_title
import reframecv.shared.generated.resources.settings_active
import reframecv.shared.generated.resources.settings_disabled
import reframecv.ui.components.settings.ProviderEditorComponent
import reframecv.ui.components.settings.runners.ProviderRunnersComponent
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderRunnersSection(component: ProviderEditorComponent) {
    val slot by component.runners.subscribeAsState()
    val provider by component.state.subscribeAsState()
    Column(
        Modifier.fillMaxWidth().padding(top = ReframeTheme.tokens.spacing.large)
            .testTag("runners.section"),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        HorizontalDivider()
        Text(stringResource(Res.string.runners_title), style = MaterialTheme.typography.titleLarge)
        val child = slot.child?.instance
        if (child == null) {
            Text(
                stringResource(Res.string.runners_save_connection),
                color = ReframeTheme.colorScheme.hint,
            )
        } else {
            ProviderRunnersList(child, enabled = !provider.busy)
        }
    }
}

@Composable
internal fun ProviderRunnersList(component: ProviderRunnersComponent, enabled: Boolean) {
    val state by component.state.subscribeAsState()
    when {
        state.loading -> LoadingContent()

        state.failed -> {
            Text(
                stringResource(Res.string.runners_load_error),
                color = ReframeTheme.colorScheme.critical,
            )
            FillButton(
                stringResource(Res.string.navigation_retry),
                component::onRetry,
                Modifier.testTag("runners.retry"),
                enabled = enabled,
            )
        }

        state.runners.isEmpty() -> Text(
            stringResource(Res.string.runners_empty),
            color = ReframeTheme.colorScheme.hint,
        )

        else -> state.runners.forEach { runner ->
            RunnerListRow(runner, enabled) { component.onEdit(runner.id) }
            HorizontalDivider()
        }
    }
    FillButton(
        stringResource(Res.string.runners_add),
        component::onAdd,
        Modifier.testTag("runners.add"),
        enabled = enabled && !state.loading,
        outlineState = FillButton.OutlineState.Visible,
    )
}

@Composable
private fun RunnerListRow(
    runner: reframecv.domain.models.providers.ConnectionRunner,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
            .padding(
                vertical = ReframeTheme.tokens.spacing.small,
            ).testTag("runners.row.${runner.id}"),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                runner.name.ifBlank {
                    operationTitle(runner.operation)
                },
            )
            if (runner.name.isNotBlank()) {
                Text(
                    operationTitle(runner.operation),
                    style = MaterialTheme.typography.bodySmall,
                    color = ReframeTheme.colorScheme.hint,
                )
            }
        }
        Text(
            stringResource(
                if (runner.active) {
                    Res.string.settings_active
                } else {
                    Res.string.settings_disabled
                },
            ),
        )
    }
}
