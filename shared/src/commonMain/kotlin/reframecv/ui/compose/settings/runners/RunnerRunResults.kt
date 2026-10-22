package reframecv.ui.compose.settings.runners

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.runners.VacancySearchResult
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.runner_cost
import reframecv.shared.generated.resources.runner_details
import reframecv.shared.generated.resources.runner_hide_details
import reframecv.shared.generated.resources.runner_next
import reframecv.shared.generated.resources.runner_posted
import reframecv.shared.generated.resources.runner_previous
import reframecv.shared.generated.resources.runner_result_count
import reframecv.shared.generated.resources.runner_results_skipped
import reframecv.shared.generated.resources.runner_run_unknown
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsState
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.settings.providerAmount
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RunnerRunResults(state: RunnerRunsState, component: RunnerRunsComponent) {
    val view = state.view ?: return
    Text("#${view.run.id} · ${view.run.state}", style = MaterialTheme.typography.titleMedium)
    view.run.externalId?.let { SelectableText(it) }
    view.remote?.costUsd?.let { Text(stringResource(Res.string.runner_cost, providerAmount(it))) }
    view.run.error?.let { SelectableText(it, color = ReframeTheme.colorScheme.critical) }
    if (state.reading) {
        LoadingContent(Modifier.height(ReframeTheme.tokens.dimensions.loadingIndicatorSize * 2))
    }
    val page = view.page
    when {
        view.run.externalId == null -> Text(stringResource(Res.string.runner_run_unknown))

        page != null -> {
            Text(
                stringResource(
                    Res.string.runner_result_count,
                    page.items.size,
                    page.total ?: page.items.size,
                ),
            )
            if (page.skipped >
                0
            ) {
                Text(
                    stringResource(Res.string.runner_results_skipped, page.skipped),
                    color = ReframeTheme.colorScheme.hint,
                )
            }
            page.items.filterIsInstance<VacancySearchResult>().forEach { VacancyCandidate(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
                if (page.offset > 0) {
                    FillButton(
                        stringResource(Res.string.runner_previous),
                        {
                            component.onPage((page.offset - RESULTS_PAGE_SIZE).coerceAtLeast(0))
                        },
                        enabled = !state.reading,
                    )
                }
                page.nextOffset?.let { next ->
                    FillButton(
                        stringResource(Res.string.runner_next),
                        {
                            component.onPage(next)
                        },
                        Modifier.testTag("runner.results.next"),
                        enabled = !state.reading,
                    )
                }
            }
        }
    }
}

@Composable
private fun VacancyCandidate(result: VacancySearchResult) {
    var expanded by remember(result.externalId) { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        modifier = Modifier.testTag("runner.result.${result.externalId}"),
    ) {
        SelectableText(result.title, style = MaterialTheme.typography.titleMedium)
        SelectableText(
            listOf(result.company, result.location).filter {
                it.isNotBlank()
            }.joinToString(" · "),
        )
        result.postedAt?.let { Text(stringResource(Res.string.runner_posted, it)) }
        if (result.salary.isNotBlank()) SelectableText(result.salary)
        FillButton(
            stringResource(
                if (expanded) Res.string.runner_hide_details else Res.string.runner_details,
            ),
            {
                expanded = !expanded
            },
            Modifier.testTag("runner.result.details.${result.externalId}"),
        )
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
                SelectableText(result.url)
                SelectableText(
                    listOf(result.employmentType, result.seniorityLevel).filter {
                        it.isNotBlank()
                    }.joinToString(" · "),
                )
                SelectableText(result.description)
            }
        }
        HorizontalDivider()
    }
}
private const val RESULTS_PAGE_SIZE = 50
