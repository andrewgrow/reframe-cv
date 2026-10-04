package reframecv.ui.compose.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

private const val MAX_CONTROL_ROWS = 2
internal const val PROJECT_CONTROLS_TAG = "projects.controls"

internal data class ProjectAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
internal fun ProjectControls(actions: List<ProjectAction>, modifier: Modifier = Modifier) {
    val tokens = ReframeTheme.tokens
    BoxWithConstraints(modifier.fillMaxWidth().padding(tokens.spacing.medium)) {
        val columnWidth = tokens.dimensions.projectActionWidth + tokens.spacing.small
        val columns = ((maxWidth + tokens.spacing.small) / columnWidth).toInt().coerceAtLeast(1)
        val rows = if (actions.size > columns) MAX_CONTROL_ROWS else 1
        val height =
            tokens.dimensions.projectActionHeight * rows + tokens.spacing.small * (rows - 1)
        LazyHorizontalGrid(
            rows = GridCells.Fixed(rows),
            modifier = Modifier.fillMaxWidth().height(height).testTag(PROJECT_CONTROLS_TAG),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            items(actions) { action ->
                FillButton(
                    text = action.label,
                    onClick = action.onClick,
                    enabled = action.enabled,
                    outlineState = FillButton.OutlineState.Visible,
                    modifier = Modifier.size(
                        width = tokens.dimensions.projectActionWidth,
                        height = tokens.dimensions.projectActionHeight,
                    ),
                )
            }
        }
    }
}
