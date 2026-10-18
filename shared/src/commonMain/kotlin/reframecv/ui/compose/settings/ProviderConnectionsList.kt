package reframecv.ui.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.ProviderConnection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_empty
import reframecv.shared.generated.resources.settings_name
import reframecv.shared.generated.resources.settings_status
import reframecv.shared.generated.resources.settings_type
import reframecv.ui.components.settings.SettingsComponent
import reframecv.ui.components.settings.SettingsState
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderConnectionsList(state: SettingsState, component: SettingsComponent) {
    if (state.loading) {
        LoadingContent()
        return
    }
    FastScrollColumn {
        if (state.connections.isEmpty() && state.error == null) {
            Text(stringResource(Res.string.settings_empty), color = ReframeTheme.colorScheme.hint)
        }
        if (state.connections.isNotEmpty()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val width = maxOf(maxWidth, ReframeTheme.tokens.dimensions.providerTableMinWidth)
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Column(Modifier.width(width)) {
                        ProviderTableHeader()
                        state.connections.forEach { ProviderConnectionRow(it, state, component) }
                    }
                }
            }
        }
        state.error?.let { SettingsErrorText(it) }
    }
}

@Composable
private fun ProviderTableHeader() {
    Row(
        Modifier.fillMaxWidth().padding(ReframeTheme.tokens.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Text(
            stringResource(Res.string.settings_type),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(Res.string.settings_name),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(Res.string.settings_status),
            Modifier.width(ReframeTheme.tokens.dimensions.providerStatusWidth),
            style = MaterialTheme.typography.titleSmall,
        )
    }
    HorizontalDivider()
}

@Composable
private fun ProviderConnectionRow(
    connection: ProviderConnection,
    state: SettingsState,
    component: SettingsComponent,
) {
    Row(
        Modifier.fillMaxWidth().clickable {
            component.onEdit(connection)
        }.padding(
            ReframeTheme.tokens.spacing.small,
        ).testTag("settings.connection.${connection.id}"),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("OpenRouter", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(connection.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        ProviderStatusField(
            connection.active,
            !component.isBusy && connection.id !in state.changingActive,
            { component.onActiveChange(connection.id, it) },
            Modifier.width(ReframeTheme.tokens.dimensions.providerStatusWidth),
        )
    }
    HorizontalDivider()
}
