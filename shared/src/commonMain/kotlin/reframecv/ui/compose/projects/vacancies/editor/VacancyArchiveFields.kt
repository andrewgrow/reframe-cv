package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.dashboard_cover_letters
import reframecv.shared.generated.resources.dashboard_resumes
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.vacancy_zip_always_included
import reframecv.shared.generated.resources.vacancy_zip_empty
import reframecv.shared.generated.resources.vacancy_zip_load_error
import reframecv.shared.generated.resources.vacancy_zip_name
import reframecv.shared.generated.resources.vacancy_zip_name_error
import reframecv.ui.components.projects.vacancies.editor.VacancyArchiveDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyExportComponent
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyArchiveFields(
    draft: VacancyArchiveDraft,
    component: VacancyExportComponent,
    busy: Boolean,
) {
    FastScrollColumn(
        Modifier.fillMaxWidth().heightIn(max = 300.dp).testTag("vacancy.export.archive.fields"),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Text(stringResource(Res.string.vacancy_zip_always_included))
        OutlinedTextField(
            draft.name,
            component::onArchiveName,
            label = { Text(stringResource(Res.string.vacancy_zip_name)) },
            enabled = !busy,
            singleLine = true,
            isError = draft.invalidName,
            supportingText = {
                Text(
                    if (draft.invalidName) {
                        stringResource(Res.string.vacancy_zip_name_error)
                    } else {
                        "${draft.filePrefix}.zip"
                    },
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("vacancy.export.archive.name"),
        )
        when {
            draft.loading -> LoadingContent(Modifier.heightIn(min = 48.dp))

            draft.failed -> {
                Text(
                    stringResource(Res.string.vacancy_zip_load_error),
                    color = ReframeTheme.colorScheme.critical,
                )
                TextButton(component::onRetryArchive, enabled = !busy) {
                    Text(stringResource(Res.string.navigation_retry))
                }
            }

            else -> ArchiveDocuments(draft, component, !busy)
        }
    }
}

@Composable
private fun ArchiveDocuments(
    draft: VacancyArchiveDraft,
    component: VacancyExportComponent,
    enabled: Boolean,
) {
    Text(
        stringResource(Res.string.dashboard_resumes) +
            " (${draft.resumeIds.size}/${draft.resumes.size})",
    )
    if (draft.resumes.isEmpty()) Text(stringResource(Res.string.vacancy_zip_empty))
    draft.resumes.forEach { resume ->
        ArchiveDocumentRow(
            resume.name,
            resume.id in draft.resumeIds,
            enabled,
            "vacancy.export.archive.resume.${resume.id}",
        ) { component.onResumeToggle(resume.id) }
    }
    Text(
        stringResource(Res.string.dashboard_cover_letters) +
            " (${draft.letterIds.size}/${draft.letters.size})",
    )
    if (draft.letters.isEmpty()) Text(stringResource(Res.string.vacancy_zip_empty))
    draft.letters.forEach { letter ->
        ArchiveDocumentRow(
            letter.name,
            letter.id in draft.letterIds,
            enabled,
            "vacancy.export.archive.letter.${letter.id}",
        ) { component.onLetterToggle(letter.id) }
    }
}

@Composable
private fun ArchiveDocumentRow(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    toggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().toggleable(selected, enabled, Role.Checkbox) { toggle() }
            .testTag(tag).padding(vertical = ReframeTheme.tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(selected, onCheckedChange = null, enabled = enabled)
        Text(name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
