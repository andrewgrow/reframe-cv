package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.attachments.AttachmentStatus
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.vacancy_attach_snapshot
import reframecv.shared.generated.resources.vacancy_snapshot_changed
import reframecv.shared.generated.resources.vacancy_snapshot_error
import reframecv.shared.generated.resources.vacancy_snapshot_hint
import reframecv.shared.generated.resources.vacancy_snapshot_loading
import reframecv.shared.generated.resources.vacancy_snapshot_missing
import reframecv.shared.generated.resources.vacancy_snapshot_open
import reframecv.shared.generated.resources.vacancy_snapshot_remove
import reframecv.shared.generated.resources.vacancy_snapshot_selected
import reframecv.ui.components.projects.vacancies.editor.VacancySnapshotComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySnapshotState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancySnapshotContent(component: VacancySnapshotComponent, enabled: Boolean = true) {
    val state by component.state.subscribeAsState()
    Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        SnapshotDescription(state)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
            TextButton(
                onClick = component::onAttach,
                enabled = enabled && !state.loading,
                contentPadding = ReframeTheme.tokens.textButtonContentPadding,
            ) {
                Text(stringResource(Res.string.vacancy_attach_snapshot))
            }
            if (state.record != null || state.selected != null) {
                if (state.selected == null && state.status == AttachmentStatus.Available) {
                    TextButton(
                        onClick = component::onOpen,
                        enabled = enabled && !state.loading,
                        contentPadding = ReframeTheme.tokens.textButtonContentPadding,
                    ) {
                        Text(stringResource(Res.string.vacancy_snapshot_open))
                    }
                }
                TextButton(
                    onClick = component::onRemove,
                    enabled = enabled && !state.loading,
                    contentPadding = ReframeTheme.tokens.textButtonContentPadding,
                ) {
                    Text(stringResource(Res.string.vacancy_snapshot_remove))
                }
            }
            if (state.failed) {
                TextButton(onClick = component::onRetry, enabled = enabled && !state.loading) {
                    Text(stringResource(Res.string.navigation_retry))
                }
            }
        }
        SelectableText(
            stringResource(Res.string.vacancy_snapshot_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
    }
}

@Composable
private fun SnapshotDescription(state: VacancySnapshotState) {
    val name = state.selected?.fileName ?: state.record?.fileName
    if (name != null) SelectableText(name, style = ReframeTheme.tokens.typography.bodyMedium)
    val message = snapshotMessage(state)
    if (message != null) {
        val critical = state.failed || state.status == AttachmentStatus.Missing ||
            state.status == AttachmentStatus.Changed
        SelectableText(
            stringResource(message),
            color = if (critical) {
                ReframeTheme.colorScheme.critical
            } else {
                ReframeTheme.colorScheme.hint
            },
            style = ReframeTheme.tokens.typography.bodySmall,
        )
    }
}

private fun snapshotMessage(state: VacancySnapshotState) = when {
    state.loading -> Res.string.vacancy_snapshot_loading
    state.failed -> Res.string.vacancy_snapshot_error
    state.selected != null -> Res.string.vacancy_snapshot_selected
    state.status == AttachmentStatus.Missing -> Res.string.vacancy_snapshot_missing
    state.status == AttachmentStatus.Changed -> Res.string.vacancy_snapshot_changed
    else -> null
}
