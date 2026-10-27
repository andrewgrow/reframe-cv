package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.runner_copy
import reframecv.shared.generated.resources.runner_copy_apply
import reframecv.shared.generated.resources.runner_copy_empty
import reframecv.shared.generated.resources.runner_copy_error
import reframecv.shared.generated.resources.runner_copy_hint
import reframecv.shared.generated.resources.runner_copy_other
import reframecv.shared.generated.resources.runner_name
import reframecv.shared.generated.resources.settings_cancel
import reframecv.ui.components.settings.runners.RunnerCopyComponent
import reframecv.ui.components.settings.runners.RunnerCopyState
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.compose.settings.ProviderStatusField
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RunnerCopyDialog(component: RunnerCopyComponent) {
    val state by component.state.subscribeAsState()
    if (!state.visible) return
    ResizableDialog(
        onDismissRequest = component::dismiss,
        title = { Text(stringResource(Res.string.runner_copy)) },
        text = {
            FastScrollColumn(
                Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                Text(stringResource(Res.string.runner_copy_hint))
                if (state.draft == null) {
                    RunnerCopySources(state, component)
                } else {
                    RunnerCopyFields(state, component)
                }
            }
        },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
                TextButton(component::dismiss, Modifier.testTag("runner.copy.cancel")) {
                    Text(stringResource(Res.string.settings_cancel))
                }
                TextButton(
                    component::apply,
                    Modifier.testTag("runner.copy.apply"),
                    enabled = state.draft != null,
                ) { Text(stringResource(Res.string.runner_copy_apply)) }
            }
        },
        modifier = Modifier.testTag("runner.copy.dialog"),
    )
}

@Composable
private fun RunnerCopySources(state: RunnerCopyState, component: RunnerCopyComponent) {
    when {
        state.loading -> LoadingContent(Modifier.heightIn(min = 60.dp))

        state.failed -> {
            Text(
                stringResource(Res.string.runner_copy_error),
                color = ReframeTheme.colorScheme.critical,
            )
            TextButton(component::open) { Text(stringResource(Res.string.navigation_retry)) }
        }

        state.sources.isEmpty() -> Text(stringResource(Res.string.runner_copy_empty))

        else -> state.sources.forEach { source ->
            Column(
                Modifier.fillMaxWidth().clickable { component.select(source.runner.id) }
                    .padding(vertical = 8.dp).testTag("runner.copy.source.${source.runner.id}"),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
            ) {
                Text(
                    source.runner.name.ifBlank {
                        operationTitle(source.runner.operation)
                    },
                )
                Text(
                    source.connection.name.ifBlank { source.connection.type.name },
                    color = ReframeTheme.colorScheme.hint,
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RunnerCopyFields(state: RunnerCopyState, component: RunnerCopyComponent) {
    val draft = state.draft ?: return
    TextButton(component::open) { Text(stringResource(Res.string.runner_copy_other)) }
    ProviderStatusField(draft.active, true, { component.edit(draft.copy(active = it)) })
    OutlinedTextField(
        draft.name,
        { component.edit(draft.copy(name = it)) },
        Modifier.fillMaxWidth().testTag("runner.copy.name"),
        singleLine = true,
        label = { Text(stringResource(Res.string.runner_name)) },
    )
    RunnerSearchFields(draft, component::edit, enabled = true, tagPrefix = "runner.copy")
}
