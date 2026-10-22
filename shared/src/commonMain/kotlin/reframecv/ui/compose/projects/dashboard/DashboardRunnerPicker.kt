package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.dashboard_runners_empty
import reframecv.shared.generated.resources.dashboard_runners_error
import reframecv.shared.generated.resources.dashboard_search_runners
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.runner_operation_linkedin
import reframecv.shared.generated.resources.settings_close
import reframecv.ui.components.projects.dashboard.DashboardRunnersComponent
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme
import reframecv.usecase.SavedRunner

@Composable
internal fun DashboardRunnerPicker(component: DashboardRunnersComponent) {
    val state by component.state.subscribeAsState()
    if (!state.visible) return
    ResizableDialog(
        onDismissRequest = component::dismiss,
        title = { Text(stringResource(Res.string.dashboard_search_runners)) },
        text = {
            FastScrollColumn(
                Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
            ) {
                when {
                    state.loading -> LoadingContent(Modifier.height(60.dp))

                    state.failed -> {
                        Text(
                            stringResource(Res.string.dashboard_runners_error),
                            color = ReframeTheme.colorScheme.critical,
                        )
                        TextButton(component::open) {
                            Text(stringResource(Res.string.navigation_retry))
                        }
                    }

                    state.runners.isEmpty() -> Text(
                        stringResource(Res.string.dashboard_runners_empty),
                    )

                    else -> state.runners.forEach { choice ->
                        RunnerChoice(choice) { component.select(choice.runner.id) }
                    }
                }
            }
        },
        actions = {
            Row {
                TextButton(component::dismiss) { Text(stringResource(Res.string.settings_close)) }
            }
        },
        modifier = Modifier.testTag("dashboard.runners.dialog"),
    )
}

@Composable
private fun RunnerChoice(choice: SavedRunner, open: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = open).padding(vertical = 8.dp)
            .testTag("dashboard.runner.${choice.runner.id}"),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Text(
            choice.runner.name.ifBlank { stringResource(Res.string.runner_operation_linkedin) },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            choice.connection.name.ifBlank {
                choice.connection.type.name
            },
            color = ReframeTheme.colorScheme.hint,
        )
        Text(
            when {
                !choice.runner.active || !choice.connection.active -> "Disabled"
                choice.available -> "Active"
                else -> "Not configured"
            },
        )
        HorizontalDivider()
    }
}
