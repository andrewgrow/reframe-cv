package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import reframecv.ui.theme.ReframeTheme

/** Keeps destructive list actions on the left and ordinary actions on the right. */
@Composable
internal fun DocumentListControls(
    showDestructive: Boolean = false,
    destructive: @Composable () -> Unit = {},
    actions: @Composable () -> Unit,
) {
    val tokens = ReframeTheme.tokens
    BoxWithConstraints(Modifier.fillMaxWidth().padding(tokens.spacing.medium)) {
        if (showDestructive && maxWidth < tokens.dimensions.documentControlsMinWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
                destructive()
                DocumentListActions(Modifier.fillMaxWidth(), actions)
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
            ) {
                if (showDestructive) destructive()
                DocumentListActions(Modifier.weight(1f), actions)
            }
        }
    }
}

@Composable
private fun DocumentListActions(modifier: Modifier, actions: @Composable () -> Unit) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(
            ReframeTheme.tokens.spacing.medium,
            Alignment.End,
        ),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) { actions() }
}
