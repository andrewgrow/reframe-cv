package reframecv.ui.compose.projects.resumes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.resume_add
import reframecv.shared.generated.resources.resumes_delete_selected
import reframecv.ui.compose.common.DocumentListControls
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumesControls(
    onAdd: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    selectedCount: Int = 0,
    enabled: Boolean = true,
) {
    DocumentListControls(
        showDestructive = onDelete != null,
        destructive = {
            onDelete?.let {
                FillButton(
                    stringResource(Res.string.resumes_delete_selected, selectedCount),
                    it,
                    Modifier.testTag("resumes.delete.selected"),
                    enabled = enabled && selectedCount > 0,
                    outlineState = FillButton.OutlineState.Visible,
                    fillColor = ReframeTheme.colorScheme.critical,
                    contentColor = ReframeTheme.colorScheme.critical,
                )
            }
        },
    ) {
        FillButton(
            text = stringResource(Res.string.resume_add),
            onClick = onAdd,
            enabled = enabled,
            outlineState = FillButton.OutlineState.Visible,
        )
        onRetry?.let { retry ->
            FillButton(
                text = stringResource(Res.string.navigation_retry),
                onClick = retry,
                enabled = enabled,
                outlineState = FillButton.OutlineState.Visible,
            )
        }
    }
}
