package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_active
import reframecv.shared.generated.resources.settings_disabled
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ProviderStatusField(
    active: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Switch(
            active,
            onChange,
            enabled = enabled,
            modifier = Modifier.testTag("settings.status.switch"),
        )
        Text(
            stringResource(
                if (active) Res.string.settings_active else Res.string.settings_disabled,
            ),
        )
    }
}
