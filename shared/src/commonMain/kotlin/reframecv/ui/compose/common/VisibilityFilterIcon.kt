package reframecv.ui.compose.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import reframecv.ui.theme.ReframeTheme

/** Shares the appearance and tooltip behavior of local visibility filters. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VisibilityFilterIcon(
    icon: ImageVector,
    text: String,
    checked: Boolean,
    enabled: Boolean,
    tag: String,
    action: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
    ) {
        IconToggleButton(
            checked,
            { action() },
            enabled = enabled,
            modifier = Modifier.size(ReframeTheme.tokens.dimensions.buttonHeight).testTag(tag),
        ) {
            Icon(
                icon,
                text,
                tint = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
