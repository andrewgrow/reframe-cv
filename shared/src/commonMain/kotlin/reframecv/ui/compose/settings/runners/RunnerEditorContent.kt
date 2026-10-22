package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.runner_limit
import reframecv.shared.generated.resources.runner_limit_hint
import reframecv.shared.generated.resources.runner_name
import reframecv.shared.generated.resources.runner_operation
import reframecv.shared.generated.resources.runner_operation_linkedin
import reframecv.shared.generated.resources.runner_title
import reframecv.shared.generated.resources.runner_urls
import reframecv.shared.generated.resources.runner_urls_hint
import reframecv.shared.generated.resources.runners_add
import reframecv.ui.components.settings.runners.RunnerEditorComponent
import reframecv.ui.components.settings.runners.RunnerEditorError
import reframecv.ui.components.settings.runners.RunnerEditorState
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.compose.settings.ProviderStatusField
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RunnerEditorContent(component: RunnerEditorComponent) {
    val slot by component.executions.subscribeAsState()
    val execution = slot.child?.instance
    if (execution != null) {
        RunnerRunsContent(execution)
        return
    }
    RunnerConfigurationContent(component)
}

@Composable
private fun RunnerConfigurationContent(component: RunnerEditorComponent) {
    val state by component.state.subscribeAsState()
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .testTag("runner.editor").pageBackHandler(
                    component::onBack,
                    destination = component,
                    enabled = !state.confirming,
                ),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                PageHeader(
                    stringResource(
                        if (state.runner.id == 0L) {
                            Res.string.runners_add
                        } else {
                            Res.string.runner_title
                        },
                    ),
                    component::onBack,
                    enabled = !state.busy,
                )
                RunnerEditorFields(state, component)
            }
            HorizontalDivider()
            RunnerFeedback(state)
            RunnerActions(state, component)
        }
        RunnerDeleteConfirmation(state, component)
    }
}

@Composable
private fun RunnerEditorFields(state: RunnerEditorState, component: RunnerEditorComponent) {
    val enabled = !state.busy && !state.confirming
    val draft = state.draft
    FastScrollColumn(
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        OutlinedTextField(
            stringResource(Res.string.runner_operation_linkedin),
            {},
            Modifier.fillMaxWidth().testTag("runner.operation"),
            readOnly = true,
            label = { Text(stringResource(Res.string.runner_operation)) },
            singleLine = true,
        )
        Text(
            "curious_coder/linkedin-jobs-scraper",
            color = ReframeTheme.colorScheme.hint,
            style = MaterialTheme.typography.bodySmall,
        )
        ProviderStatusField(
            draft.active,
            enabled,
            { component.onDraft(draft.copy(active = it)) },
            Modifier.testTag("runner.active"),
        )
        OutlinedTextField(
            draft.name,
            { component.onDraft(draft.copy(name = it)) },
            Modifier.fillMaxWidth().testTag("runner.name"),
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(Res.string.runner_name)) },
        )
        RunnerSearchFields(state, component, enabled)
    }
}

@Composable
private fun RunnerSearchFields(
    state: RunnerEditorState,
    component: RunnerEditorComponent,
    enabled: Boolean,
) {
    val draft = state.draft
    val validation = state.error == RunnerEditorError.Validation
    OutlinedTextField(
        draft.searchUrls,
        { component.onDraft(draft.copy(searchUrls = it)) },
        Modifier.fillMaxWidth().testTag("runner.urls"),
        enabled = enabled,
        minLines = 4,
        label = { Text(stringResource(Res.string.runner_urls)) },
        supportingText = { Text(stringResource(Res.string.runner_urls_hint)) },
        isError = validation && !draft.validUrls,
    )
    OutlinedTextField(
        draft.maxResults,
        { component.onDraft(draft.copy(maxResults = it)) },
        Modifier.fillMaxWidth().testTag("runner.limit"),
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(Res.string.runner_limit)) },
        supportingText = { Text(stringResource(Res.string.runner_limit_hint)) },
        isError = validation && !draft.validLimit,
    )
}
