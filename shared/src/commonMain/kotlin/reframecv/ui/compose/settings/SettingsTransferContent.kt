package reframecv.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_transfer_description
import reframecv.shared.generated.resources.settings_transfer_export
import reframecv.shared.generated.resources.settings_transfer_export_error
import reframecv.shared.generated.resources.settings_transfer_exported
import reframecv.shared.generated.resources.settings_transfer_import
import reframecv.shared.generated.resources.settings_transfer_import_error
import reframecv.shared.generated.resources.settings_transfer_imported
import reframecv.shared.generated.resources.settings_transfer_keys
import reframecv.shared.generated.resources.settings_transfer_secret_warning
import reframecv.shared.generated.resources.settings_transfer_title
import reframecv.shared.generated.resources.settings_transfer_window
import reframecv.ui.components.settings.SettingsTransferComponent
import reframecv.ui.components.settings.SettingsTransferOperation
import reframecv.ui.components.settings.SettingsTransferState
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun SettingsTransferContent(component: SettingsTransferComponent) {
    val state by component.state.subscribeAsState()
    val spacing = ReframeTheme.tokens.spacing
    val enabled = state.operation == null
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(spacing.medium).testTag("settings.transfer.page"),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            PageHeader(stringResource(Res.string.settings_transfer_title), component::onBack)
            FastScrollColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                SettingsTransferOptions(component, state)
                SettingsTransferFeedback(state)
                if (!enabled) {
                    CircularProgressIndicator(
                        Modifier.testTag("settings.transfer.progress"),
                    )
                }
            }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End),
            ) {
                FillButton(
                    stringResource(Res.string.settings_transfer_import),
                    component::onImport,
                    Modifier.testTag("settings.transfer.import"),
                    enabled = enabled,
                )
                FillButton(
                    stringResource(Res.string.settings_transfer_export),
                    component::onExport,
                    Modifier.testTag("settings.transfer.export"),
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun SettingsTransferOptions(
    component: SettingsTransferComponent,
    state: SettingsTransferState,
) {
    val enabled = state.operation == null
    Text(stringResource(Res.string.settings_transfer_description))
    Row(
        Modifier.toggleable(
            state.includeApiKeys,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = component::onIncludeApiKeys,
        ).testTag("settings.transfer.keys"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Checkbox(state.includeApiKeys, onCheckedChange = null, enabled = enabled)
        Text(stringResource(Res.string.settings_transfer_keys))
    }
    if (state.includeApiKeys) {
        Text(
            stringResource(Res.string.settings_transfer_secret_warning),
            color = ReframeTheme.colorScheme.critical,
            modifier = Modifier.testTag("settings.transfer.warning"),
        )
    }
}

@Composable
private fun SettingsTransferFeedback(state: SettingsTransferState) {
    val error = when (state.error) {
        SettingsTransferOperation.Import -> Res.string.settings_transfer_import_error
        SettingsTransferOperation.Export -> Res.string.settings_transfer_export_error
        null -> null
    }
    if (error != null) {
        Text(
            stringResource(error),
            color = ReframeTheme.colorScheme.critical,
            modifier = Modifier.testTag("settings.transfer.error"),
        )
    }
    if (state.exported) {
        Text(
            stringResource(Res.string.settings_transfer_exported),
            color = ReframeTheme.colorScheme.success,
        )
    }
    state.report?.let {
        Text(
            stringResource(
                Res.string.settings_transfer_imported,
                it.connections,
                it.prompts,
                it.duplicates,
                it.skipped,
            ),
            color = ReframeTheme.colorScheme.success,
            modifier = Modifier.testTag("settings.transfer.result"),
        )
        if (it.windowRestored) Text(stringResource(Res.string.settings_transfer_window))
    }
}
