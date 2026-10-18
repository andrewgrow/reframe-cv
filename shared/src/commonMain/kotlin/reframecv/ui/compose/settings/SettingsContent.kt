package reframecv.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import reframecv.shared.generated.resources.prompts_title
import reframecv.shared.generated.resources.settings_account_error
import reframecv.shared.generated.resources.settings_error_auth
import reframecv.shared.generated.resources.settings_error_disabled
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
import reframecv.ui.components.settings.SettingsSection
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.compose.settings.prompts.PromptsContent
import reframecv.ui.theme.ReframeTheme

@Composable
fun SettingsContent(component: SettingsComponent) {
    val state by component.state.subscribeAsState()
    val editor by component.editorSlot.subscribeAsState()
    val child = editor.child?.instance
    val prompts by component.promptsSlot.subscribeAsState()
    val promptsChild = prompts.child?.instance
    Box(
        Modifier.fillMaxSize().pageBackHandler(
            component::onBack,
            destination =
                child ?: promptsChild ?: state.section,
        ),
    ) {
        when {
            child != null -> ProviderEditorContent(child)
            promptsChild != null -> PromptsContent(promptsChild)
            state.section == SettingsSection.ApiProviders -> ProviderConnectionsContent(component)
            else -> SettingsListContent(component)
        }
    }
}

@Composable
private fun SettingsListContent(component: SettingsComponent) {
    val spacing = ReframeTheme.tokens.spacing
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(spacing.medium).testTag("settings.page"),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            PageHeader(stringResource(Res.string.settings_title), component::onBack)
            FastScrollColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                SettingsNavigationItem(
                    stringResource(Res.string.settings_providers),
                    component::onProviders,
                    "settings.providers",
                )
                SettingsNavigationItem(
                    stringResource(Res.string.prompts_title),
                    component::onPrompts,
                    "settings.prompts",
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(title: String, onClick: () -> Unit, tag: String) {
    Text(
        "$title  ›",
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(ReframeTheme.tokens.spacing.small).testTag(tag),
        style = MaterialTheme.typography.titleMedium,
    )
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
        SettingsError.Disabled -> Res.string.settings_error_disabled
    }
    Text(
        stringResource(message),
        color = ReframeTheme.colorScheme.critical,
        modifier = Modifier.testTag("settings.error"),
    )
}
