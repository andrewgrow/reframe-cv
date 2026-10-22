package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.runner_history_empty
import reframecv.shared.generated.resources.runner_history_error
import reframecv.shared.generated.resources.runner_operation_linkedin
import reframecv.shared.generated.resources.runner_refresh
import reframecv.shared.generated.resources.runner_runs
import reframecv.shared.generated.resources.runner_runs_notice
import reframecv.shared.generated.resources.runner_start
import reframecv.shared.generated.resources.settings_close
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsState
import reframecv.ui.compose.common.DocumentListControls
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RunnerRunsContent(component: RunnerRunsComponent) {
    val state by component.state.subscribeAsState()
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .testTag(
                    "runner.runs.page",
                ).pageBackHandler(component::onBack, destination = state.view?.run?.id),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                PageHeader(
                    stringResource(Res.string.runner_runs),
                    component::onBack,
                    enabled = !state.starting && !state.working,
                )
                Text(
                    state.runner.name.ifBlank {
                        stringResource(Res.string.runner_operation_linkedin)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(Res.string.runner_runs_notice),
                    color = ReframeTheme.colorScheme.hint,
                )
                state.error?.let { SelectableText(it, color = ReframeTheme.colorScheme.critical) }
                state.notice?.let { Text(it, color = ReframeTheme.colorScheme.success) }
                if (state.view != null) {
                    RunnerRunResults(state, component, Modifier.weight(1f))
                } else {
                    FastScrollColumn(
                        Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
                    ) {
                        if (state.starting) {
                            LoadingContent(
                                Modifier.height(
                                    ReframeTheme.tokens.dimensions.loadingIndicatorSize * 2,
                                ),
                            )
                        }
                        RunnerHistory(state, component)
                    }
                }
            }
            HorizontalDivider()
            RunnerRunControls(state, component)
            RunnerImportDialog(state, component)
        }
    }
}

@Composable
private fun RunnerHistory(state: RunnerRunsState, component: RunnerRunsComponent) {
    when {
        state.loadingHistory -> LoadingContent(
            Modifier.height(ReframeTheme.tokens.dimensions.loadingIndicatorSize * 2),
        )

        state.historyFailed -> {
            Text(
                stringResource(Res.string.runner_history_error),
                color = ReframeTheme.colorScheme.critical,
            )
            FillButton(stringResource(Res.string.navigation_retry), component::onRetryHistory)
        }

        state.runs.isEmpty() -> Text(stringResource(Res.string.runner_history_empty))

        else -> state.runs.forEach { run ->
            Column(
                Modifier.fillMaxWidth().clickable(enabled = !state.starting) {
                    component.onSelect(run.id)
                }
                    .padding(
                        vertical = ReframeTheme.tokens.spacing.small,
                    ).testTag("runner.run.${run.id}"),
            ) {
                Text("#${run.id} · ${run.state}", style = MaterialTheme.typography.titleMedium)
                Text(
                    run.startedAt.let { kotlin.time.Instant.fromEpochMilliseconds(it).toString() },
                    color = ReframeTheme.colorScheme.hint,
                )
                run.externalId?.let {
                    SelectableText(it, style = MaterialTheme.typography.bodySmall)
                }
                run.error?.let { SelectableText(it, color = ReframeTheme.colorScheme.critical) }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun RunnerRunControls(state: RunnerRunsState, component: RunnerRunsComponent) {
    DocumentListControls {
        if (state.view !=
            null
        ) {
            FillButton(
                stringResource(Res.string.runner_refresh),
                component::onRefresh,
                Modifier.testTag("runner.run.refresh"),
                enabled = !state.starting && !state.reading && !state.working,
            )
        }
        FillButton(
            stringResource(Res.string.runner_start),
            component::onStart,
            Modifier.testTag("runner.run.start"),
            enabled = !state.starting && !state.working && state.runner.active,
            outlineState = FillButton.OutlineState.Visible,
        )
        FillButton(
            stringResource(Res.string.settings_close),
            component::onBack,
            Modifier.testTag("runner.runs.close"),
            enabled = !state.starting && !state.working,
        )
    }
}
