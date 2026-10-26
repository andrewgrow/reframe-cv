package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.ui.components.settings.runners.RunnerResultAction
import reframecv.ui.components.settings.runners.RunnerRunsComponent

@Composable
internal fun RunnerResultActions(
    result: RunnerResult,
    component: RunnerRunsComponent,
    enabled: Boolean,
) {
    Row {
        val imported = result.reviewStatus == RunnerResultStatus.Imported
        val dismissed = result.reviewStatus == RunnerResultStatus.Dismissed
        when {
            imported -> ResultIcon(
                Icons.Default.CheckCircle,
                result.reviewVacancyId?.let { "Imported into vacancy #$it" }
                    ?: "Imported (vacancy deleted)",
            )

            dismissed -> ResultIcon(Icons.Default.Block, "Dismissed")

            result.previousResultId != null -> ResultIcon(
                Icons.Default.ContentCopy,
                "Seen before",
            )
        }
        if (!imported) {
            if (!dismissed) {
                ResultIcon(
                    Icons.Default.AddCircleOutline,
                    "Convert to vacancy",
                    enabled,
                    "runner.result.convert.${result.id}",
                ) {
                    component.onResultAction(result.id, RunnerResultAction.Convert)
                }
            }
            ResultIcon(
                if (dismissed) Icons.Default.Undo else Icons.Default.Block,
                if (dismissed) "Restore result" else "Dismiss result",
                enabled,
                "runner.result.review.${result.id}",
            ) {
                component.onResultAction(
                    result.id,
                    if (dismissed) RunnerResultAction.Restore else RunnerResultAction.Dismiss,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultIcon(
    icon: ImageVector,
    text: String,
    enabled: Boolean = false,
    tag: String = text,
    action: () -> Unit = {},
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
    ) {
        IconButton(action, enabled = enabled, modifier = Modifier.size(32.dp).testTag(tag)) {
            Icon(icon, text, Modifier.size(20.dp))
        }
    }
}
