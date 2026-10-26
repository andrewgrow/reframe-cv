package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
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
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.runner_hide_dismissed
import reframecv.shared.generated.resources.runner_hide_imported
import reframecv.shared.generated.resources.runner_show_dismissed
import reframecv.shared.generated.resources.runner_show_imported
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RunnerResultFilters(state: RunnerRunsState, component: RunnerRunsComponent) {
    val filter = state.filter
    val enabled = !state.reading && !component.isBusy
    ResultFilterIcon(
        Icons.Default.Block,
        stringResource(
            if (filter.showDismissed) {
                Res.string.runner_hide_dismissed
            } else {
                Res.string.runner_show_dismissed
            },
        ),
        !filter.showDismissed,
        enabled,
        "runner.filter.dismissed",
    ) { component.onFilter(filter.copy(showDismissed = !filter.showDismissed)) }
    ResultFilterIcon(
        Icons.Default.CheckCircle,
        stringResource(
            if (filter.showImported) {
                Res.string.runner_hide_imported
            } else {
                Res.string.runner_show_imported
            },
        ),
        !filter.showImported,
        enabled,
        "runner.filter.imported",
    ) { component.onFilter(filter.copy(showImported = !filter.showImported)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultFilterIcon(
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
