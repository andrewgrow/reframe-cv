package reframecv.ui.compose.projects.resumes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import reframecv.ui.components.projects.resumes.ResumesComponent
import reframecv.ui.components.projects.resumes.ResumesState

@Composable
internal fun ResumesListControls(component: ResumesComponent) {
    val state by component.uiState.subscribeAsState()
    val selection by component.selection.state.subscribeAsState()
    ResumesControls(
        onAdd = component::onAdd,
        onDelete = if (state is ResumesState.Ready) {
            component.selection::onRequestDelete
        } else {
            null
        },
        selectedCount = selection.selected.size,
        enabled = !selection.confirming && !selection.deleting,
        onRetry = if (state == ResumesState.LoadFailed) component::onRetry else null,
    )
}
