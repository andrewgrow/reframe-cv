package reframecv.ui.compose.projects.empty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_add
import reframecv.shared.generated.resources.project_configure
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun EmptyProjectActions(onAddProject: () -> Unit, onConfigureProject: () -> Unit = {}) {
    val tokens = ReframeTheme.tokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(
            tokens.spacing.small,
            Alignment.CenterHorizontally,
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
