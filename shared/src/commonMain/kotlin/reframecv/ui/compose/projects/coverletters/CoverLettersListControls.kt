package reframecv.ui.compose.projects.coverletters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.cover_letters_delete_selected
import reframecv.shared.generated.resources.letter_add
import reframecv.shared.generated.resources.navigation_retry
import reframecv.ui.components.projects.coverletters.CoverLettersComponent
import reframecv.ui.components.projects.coverletters.CoverLettersState
import reframecv.ui.compose.common.DocumentListControls
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun CoverLettersListControls(component: CoverLettersComponent) {
    val state by component.uiState.subscribeAsState()
    val selection by component.selection.state.subscribeAsState()
    DocumentListControls(
        showDestructive = state is CoverLettersState.Ready,
        destructive = {
            if (state is CoverLettersState.Ready) {
                FillButton(
                    stringResource(
                        Res.string.cover_letters_delete_selected,
                        selection.selected.size,
                    ),
                    component.selection::onRequestDelete,
                    Modifier.testTag("coverletters.delete.selected"),
                    enabled =
                        selection.selected.isNotEmpty() && !selection.confirming &&
                            !selection.deleting,
                    outlineState = FillButton.OutlineState.Visible,
                    fillColor = ReframeTheme.colorScheme.critical,
                    contentColor = ReframeTheme.colorScheme.critical,
                )
            }
        },
    ) {
        FillButton(
            stringResource(Res.string.letter_add),
            component::onAdd,
            enabled = !selection.confirming && !selection.deleting,
            outlineState = FillButton.OutlineState.Visible,
            modifier = Modifier.testTag("coverletters.add"),
        )
        if (state == CoverLettersState.LoadFailed) {
            FillButton(
                stringResource(Res.string.navigation_retry),
                component::onRetry,
                outlineState = FillButton.OutlineState.Visible,
            )
        }
    }
}
