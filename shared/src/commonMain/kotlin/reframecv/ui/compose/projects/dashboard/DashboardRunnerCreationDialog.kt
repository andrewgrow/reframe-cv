package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import reframecv.shared.generated.resources.dashboard_runner_connection
import reframecv.shared.generated.resources.dashboard_runner_connections_empty
import reframecv.shared.generated.resources.dashboard_runner_connections_error
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.settings_close
import reframecv.ui.components.projects.dashboard.DashboardRunnerCreationComponent
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun DashboardRunnerCreationDialog(component: DashboardRunnerCreationComponent) {
    val state by component.state.subscribeAsState()
    if (!state.visible) return
    ResizableDialog(
        onDismissRequest = component::dismiss,
        title = { Text(stringResource(Res.string.dashboard_runner_connection)) },
        text = {
            FastScrollColumn(
                Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
            ) {
                when {
                    state.loading -> LoadingContent(Modifier.height(60.dp))

                    state.failed -> {
                        Text(
                            stringResource(Res.string.dashboard_runner_connections_error),
                            color = ReframeTheme.colorScheme.critical,
                        )
                        TextButton(component::open) {
                            Text(stringResource(Res.string.navigation_retry))
                        }
                    }

                    state.connections.isEmpty() -> Text(
                        stringResource(Res.string.dashboard_runner_connections_empty),
                    )

                    else -> state.connections.forEach { connection ->
                        Column(
                            Modifier.fillMaxWidth().clickable { component.select(connection.id) }
                                .padding(vertical = 8.dp)
                                .testTag("dashboard.runner.connection.${connection.id}"),
                            verticalArrangement = Arrangement.spacedBy(
                                ReframeTheme.tokens.spacing.small,
                            ),
                        ) {
                            Text(connection.name.ifBlank { connection.type.name })
                            if (connection.name.isNotBlank()) {
                                Text(connection.type.name, color = ReframeTheme.colorScheme.hint)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(component::dismiss) { Text(stringResource(Res.string.settings_close)) }
        },
        modifier = Modifier.testTag("dashboard.runner.connections"),
    )
}
