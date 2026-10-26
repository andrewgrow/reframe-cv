package reframecv.ui.compose.settings.runners

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.runner_hide_dismissed
import reframecv.shared.generated.resources.runner_hide_imported
import reframecv.shared.generated.resources.runner_show_dismissed
import reframecv.shared.generated.resources.runner_show_imported
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsState
import reframecv.ui.compose.common.VisibilityFilterIcon

@Composable
internal fun RunnerResultFilters(state: RunnerRunsState, component: RunnerRunsComponent) {
    val filter = state.filter
    val enabled = !state.reading && !component.isBusy
    VisibilityFilterIcon(
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
    VisibilityFilterIcon(
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
