package reframecv.ui.compose.projects.empty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.project_add
import reframecv.shared.generated.resources.project_configure
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

private const val ACTION_COUNT = 3

@Composable
internal fun EmptyProjectActions(
    onAddProject: () -> Unit,
    onConfigureProject: () -> Unit = {},
    onDeleteProject: () -> Unit = {},
) {
    val tokens = ReframeTheme.tokens
    val delete: @Composable () -> Unit = {
        FillButton(
            text = stringResource(Res.string.action_delete),
            onClick = onDeleteProject,
            outlineState = FillButton.OutlineState.Visible,
            fillColor = ReframeTheme.colorScheme.critical,
            contentColor = ReframeTheme.colorScheme.critical,
            filledContentColor = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(
                tokens.dimensions.projectActionWidth,
                tokens.dimensions.projectActionHeight,
            ),
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >=
            tokens.dimensions.projectActionWidth * ACTION_COUNT + tokens.spacing.small * 2
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            ) {
                delete()
                Spacer(Modifier.weight(1f))
                EmptyProjectPrimaryActions(onAddProject, onConfigureProject, Modifier)
            }
        } else {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            ) {
                delete()
                EmptyProjectPrimaryActions(onAddProject, onConfigureProject)
            }
        }
    }
}

@Composable
private fun EmptyProjectPrimaryActions(
    onAddProject: () -> Unit,
    onConfigureProject: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val tokens = ReframeTheme.tokens
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            tokens.spacing.small,
            Alignment.End,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        FillButton(
            text = stringResource(Res.string.project_configure),
            onClick = onConfigureProject,
            outlineState = FillButton.OutlineState.Visible,
            modifier = Modifier.size(
                tokens.dimensions.projectActionWidth,
                tokens.dimensions.projectActionHeight,
            ),
        )
        FillButton(
            text = stringResource(Res.string.project_add),
            onClick = onAddProject,
            outlineState = FillButton.OutlineState.Visible,
            modifier = Modifier.size(
                tokens.dimensions.projectActionWidth,
                tokens.dimensions.projectActionHeight,
            ),
        )
    }
}
