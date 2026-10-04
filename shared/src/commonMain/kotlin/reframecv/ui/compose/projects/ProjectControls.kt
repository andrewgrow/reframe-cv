package reframecv.ui.compose.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.Spacing

private const val CONTROL_WIDTH = 200
private const val CONTROL_HEIGHT = 48
private const val MAX_CONTROL_ROWS = 2
internal const val PROJECT_CONTROLS_TAG = "projects.controls"

internal data class ProjectAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
internal fun ProjectControls(actions: List<ProjectAction>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(Spacing.medium)) {
        val columns = ((maxWidth + Spacing.small) / (CONTROL_WIDTH.dp + Spacing.small))
            .toInt().coerceAtLeast(1)
        val rows = if (actions.size > columns) MAX_CONTROL_ROWS else 1
        val height = CONTROL_HEIGHT.dp * rows + Spacing.small * (rows - 1)
        LazyHorizontalGrid(
            rows = GridCells.Fixed(rows),
            modifier = Modifier.fillMaxWidth().height(height).testTag(PROJECT_CONTROLS_TAG),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            items(actions) { action ->
                FillButton(
                    text = action.label,
                    onClick = action.onClick,
                    enabled = action.enabled,
                    outlineState = FillButton.OutlineState.Visible,
                    modifier = Modifier.width(CONTROL_WIDTH.dp),
                )
            }
        }
    }
}
