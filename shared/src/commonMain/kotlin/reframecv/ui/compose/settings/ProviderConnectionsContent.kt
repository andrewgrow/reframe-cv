package reframecv.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.settings_add
import reframecv.shared.generated.resources.settings_providers
import reframecv.ui.components.settings.SettingsComponent
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderConnectionsContent(component: SettingsComponent) {
    val state by component.state.subscribeAsState()
    val spacing = ReframeTheme.tokens.spacing
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.background,
            ).testTag("settings.providers.page"),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                PageHeader(stringResource(Res.string.settings_providers), component::onBack)
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
