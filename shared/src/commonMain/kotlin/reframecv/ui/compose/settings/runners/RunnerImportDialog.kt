package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsState
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RunnerImportDialog(state: RunnerRunsState, component: RunnerRunsComponent) {
    val result = state.importResultId ?: return
    var selected by remember(result) { mutableStateOf<Long?>(null) }
    ResizableDialog(
        onDismissRequest = { if (!state.working) component.onProject(null) },
        resizingEnabled = !state.working,
        title = { Text("Create vacancy in project") },
        text = {
            FastScrollColumn(Modifier.height(300.dp)) {
                if (state.projects.isEmpty()) Text("Create a project without subprojects first.")
                state.projects.forEach { project ->
                    Row(
                        Modifier.clickable(enabled = !state.working) { selected = project.id }
                            .testTag("runner.import.project.${project.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected == project.id, {
                            selected = project.id
                        }, enabled = !state.working)
                        Text("${project.name} (#${project.id})")
                    }
                }
                state.error?.let { Text(it, color = ReframeTheme.colorScheme.critical) }
            }
        },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
                TextButton(
                    { component.onProject(null) },
                    enabled = !state.working,
                    modifier = Modifier.testTag("runner.import.cancel"),
                ) { Text("Cancel") }
                TextButton(
                    { component.onProject(selected) },
                    enabled =
                        !state.working && selected != null,
                    modifier = Modifier.testTag("runner.import.confirm"),
                ) { Text("Create vacancy") }
            }
        },
        modifier = Modifier.testTag("runner.import.dialog"),
    )
}
