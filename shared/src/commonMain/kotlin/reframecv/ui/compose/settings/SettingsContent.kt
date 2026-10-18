package reframecv.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.settings_account_error
import reframecv.shared.generated.resources.settings_activate
import reframecv.shared.generated.resources.settings_active
import reframecv.shared.generated.resources.settings_add
import reframecv.shared.generated.resources.settings_empty
import reframecv.shared.generated.resources.settings_error_auth
import reframecv.shared.generated.resources.settings_error_empty
import reframecv.shared.generated.resources.settings_error_load
import reframecv.shared.generated.resources.settings_error_network
import reframecv.shared.generated.resources.settings_error_response
import reframecv.shared.generated.resources.settings_error_save
import reframecv.shared.generated.resources.settings_error_validation
import reframecv.shared.generated.resources.settings_providers
import reframecv.shared.generated.resources.settings_title
import reframecv.ui.components.settings.SettingsComponent
import reframecv.ui.components.settings.SettingsError
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

@Composable
fun SettingsContent(component: SettingsComponent) {
    val editor by component.editorSlot.subscribeAsState()
    val child = editor.child?.instance
    Box(
        Modifier.fillMaxSize().pageBackHandler(component::onBack, destination = child ?: component),
    ) {
        if (child != null) ProviderEditorContent(child) else SettingsListContent(component)
    }
}

@Composable
private fun SettingsListContent(component: SettingsComponent) {
    val state by component.state.subscribeAsState()
    val spacing = ReframeTheme.tokens.spacing
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.background,
            ).testTag("settings.page"),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                PageHeader(stringResource(Res.string.settings_title), component::onBack)
                Text(
                    stringResource(Res.string.settings_providers),
                    style = MaterialTheme.typography.titleMedium,
                )
                ProviderConnectionsList(state, component)
            }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(spacing.medium),
                horizontalArrangement = Arrangement.Center,
            ) {
                FillButton(
                    stringResource(Res.string.settings_add),
                    component::onAdd,
                    Modifier.testTag("settings.add"),
                    outlineState = FillButton.OutlineState.Visible,
                )
                if (state.error !=
                    null
                ) {
                    FillButton(stringResource(Res.string.navigation_retry), component::onRetry)
                }
            }
        }
    }
}

@Composable
internal fun SettingsErrorText(error: SettingsError) {
    val message = when (error) {
        SettingsError.Load -> Res.string.settings_error_load
        SettingsError.Save -> Res.string.settings_error_save
        SettingsError.Unauthorized -> Res.string.settings_error_auth
        SettingsError.Unavailable -> Res.string.settings_error_network
        SettingsError.InvalidResponse -> Res.string.settings_error_response
        SettingsError.NoModels -> Res.string.settings_error_empty
        SettingsError.Validation -> Res.string.settings_error_validation
        SettingsError.AccountRefresh -> Res.string.settings_account_error
    }
    Text(
        stringResource(message),
        color = ReframeTheme.colorScheme.critical,
        modifier = Modifier.testTag("settings.error"),
    )
}

@Composable
private fun ProviderConnectionsList(
    state: reframecv.ui.components.settings.SettingsState,
    component: SettingsComponent,
) {
    val spacing = ReframeTheme.tokens.spacing
    if (state.loading) {
        LoadingContent()
    } else {
        FastScrollColumn {
            if (state.connections.isEmpty() && state.error == null) {
                Text(
                    stringResource(Res.string.settings_empty),
                    color = ReframeTheme.colorScheme.hint,
                )
            }
            state.connections.forEach { connection ->
                Column(
                    Modifier.fillMaxWidth().clickable {
                        component.onEdit(connection)
                    }.padding(
                        spacing.small,
                    ).testTag("settings.connection.${connection.id}"),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    Text(connection.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull("OpenRouter", connection.modelId.takeIf(String::isNotBlank))
                            .joinToString(" · "),
                        color = ReframeTheme.colorScheme.hint,
                    )
                    if (connection.active) {
                        Text(
                            stringResource(Res.string.settings_active),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        FillButton(stringResource(Res.string.settings_activate), {
                            component.onActivate(connection.id)
                        })
                    }
                }
                HorizontalDivider()
            }
            state.error?.let { SettingsErrorText(it) }
        }
    }
}
