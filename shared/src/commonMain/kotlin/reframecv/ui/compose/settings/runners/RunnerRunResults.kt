package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsState
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.settings.providerAmount
import reframecv.ui.theme.ReframeTheme
import reframecv.usecase.RunnerDatasetUseCases

@Composable
internal fun RunnerRunResults(
    state: RunnerRunsState,
    component: RunnerRunsComponent,
    modifier: Modifier = Modifier,
) {
    val view = state.view ?: return
    val page = view.page
    val enabled = !state.reading && !state.working
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("#${view.run.id} · ${view.run.state}")
            (view.run.costUsd ?: view.remote?.costUsd)?.let {
                Text("Run cost: $" + providerAmount(it))
            }
        }
        if (state.reading || state.working) LoadingContent(Modifier.height(40.dp))
        when {
            page != null -> {
                Text(
                    "Saved ${page.savedCount} · Read ${view.run.nextResultOffset} " +
                        "of ${view.run.resultTotal ?: "?"}",
                )
                key(view.run.id, page.offset) {
                    RunnerResultsTable(
                        page.savedResults,
                        view.run.resultColumns,
                        component,
                        enabled,
                        Modifier.weight(1f),
                    )
                }
                RunnerResultPagination(state, component)
            }

            view.run.externalId == null -> Text(
                "No remote run ID is available. Check Apify Console before starting again.",
            )

            else -> Text("Waiting for results…")
        }
    }
}

@Composable
private fun RunnerResultPagination(state: RunnerRunsState, component: RunnerRunsComponent) {
    val view = state.view ?: return
    val page = view.page ?: return
    val size = RunnerDatasetUseCases.REVIEW_PAGE_SIZE
    val enabled = !state.reading && !state.working
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FillButton(
            "Previous",
            { component.onPage((page.offset - size).coerceAtLeast(0)) },
            enabled = enabled && page.offset > 0,
            modifier = Modifier.testTag("runner.results.previous"),
        )
        val number = page.offset / size + 1
        val totalPages = ((page.savedCount + size - 1) / size).coerceAtLeast(1)
        Box(
            Modifier.height(ReframeTheme.tokens.dimensions.buttonHeight),
            contentAlignment = Alignment.Center,
        ) { Text("Page $number of $totalPages") }
        FillButton(
            "Next",
            { page.nextOffset?.let(component::onPage) },
            enabled = enabled && page.nextOffset != null,
            modifier = Modifier.testTag("runner.results.next"),
        )
        FillButton(
            "Load more",
            component::onLoadMore,
            enabled =
                enabled && !view.run.resultsComplete,
            modifier = Modifier.testTag("runner.results.more"),
            outlineState = FillButton.OutlineState.Visible,
        )
    }
}
