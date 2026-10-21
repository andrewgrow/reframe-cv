package reframecv.ui.compose.projects.resumes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.ResumeVacancyLink
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_added
import reframecv.shared.generated.resources.resume_name
import reframecv.shared.generated.resources.resumes_linked
import reframecv.shared.generated.resources.resumes_select
import reframecv.shared.generated.resources.resumes_select_all
import reframecv.ui.compose.common.documentDate
import reframecv.ui.theme.ReframeTheme

/** Separates checkbox interaction from opening a resume. */
internal data class ResumeRowSelection(
    val checked: Boolean,
    val enabled: Boolean,
    val toggle: () -> Unit,
)

/** Supplies all vacancy references and their independent navigation callback. */
internal data class ResumeRowLinks(val records: List<ResumeVacancyLink>, val open: (Long) -> Unit)

@Composable
internal fun ResumeRow(
    resume: Resume,
    timeZone: TimeZone,
    table: Boolean,
    selection: ResumeRowSelection,
    links: ResumeRowLinks,
    onEdit: () -> Unit,
) {
    var showLinks by remember(resume.id) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().testTag("resumes.row.${resume.id}"),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResumeRowCheckbox(resume, selection)
        if (table) {
            SelectableText(
                resume.name,
                modifier = resumeTextClick(onEdit),
                selectionModifier = Modifier.weight(2f),
                style = MaterialTheme.typography.bodyLarge,
            )
            ResumeVacancyLinks(links, selection.enabled, { showLinks = true }, Modifier.weight(1f))
            SelectableText(
                documentDate(resume.createdAt, timeZone),
                modifier = resumeTextClick(onEdit),
                selectionModifier = Modifier.width(
                    ReframeTheme.tokens.dimensions.documentDateWidth,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
            ) {
                SelectableText(
                    resume.name,
                    modifier = resumeTextClick(onEdit),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ResumeVacancyLinks(links, selection.enabled, {
                    showLinks = true
                }, Modifier.fillMaxWidth())
                SelectableText(
                    documentDate(resume.createdAt, timeZone),
                    modifier = resumeTextClick(onEdit),
                    color = ReframeTheme.colorScheme.hint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (showLinks && selection.enabled) {
        ResumeVacanciesDialog(links, resume.name, onClose = { showLinks = false })
    }
}

@Composable
internal fun ResumeTableHeader(state: ToggleableState, enabled: Boolean, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResumeSelectAll(state, enabled, toggle)
        SelectableText(
            stringResource(Res.string.resume_name),
            selectionModifier = Modifier.weight(2f),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.resumes_linked),
            selectionModifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.resume_added),
            selectionModifier = Modifier.width(ReframeTheme.tokens.dimensions.documentDateWidth),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun ResumeSelectAll(state: ToggleableState, enabled: Boolean, toggle: () -> Unit) {
    val label = stringResource(Res.string.resumes_select_all)
    TriStateCheckbox(
        state,
        toggle,
        enabled = enabled,
        modifier = Modifier.size(
            ReframeTheme.tokens.dimensions.iconButtonSize,
        ).testTag("resumes.select.all").semantics {
            contentDescription =
                label
        },
    )
}

@Composable
private fun resumeTextClick(onEdit: () -> Unit): Modifier = Modifier.clickable(
    interactionSource = remember {
        MutableInteractionSource()
    },
    indication = null,
    role = Role.Button,
    onClick = onEdit,
)

@Composable
private fun ResumeRowCheckbox(resume: Resume, selection: ResumeRowSelection) {
    val label = stringResource(Res.string.resumes_select, resume.name)
    Checkbox(
        selection.checked,
        { selection.toggle() },
        enabled = selection.enabled,
        modifier = Modifier.size(
            ReframeTheme.tokens.dimensions.iconButtonSize,
        ).testTag("resumes.select.${resume.id}").semantics {
            contentDescription =
                label
        },
    )
}
