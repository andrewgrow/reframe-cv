package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_applied
import reframecv.shared.generated.resources.vacancy_applied_at
import reframecv.shared.generated.resources.vacancy_applied_format
import reframecv.shared.generated.resources.vacancy_set_now
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.appliedTimeText
import reframecv.ui.components.projects.vacancies.editor.validAppliedTime
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyAppliedFields(
    draft: VacancyDraft,
    onChange: (VacancyDraft) -> Unit,
    enabled: Boolean,
) {
    val tokens = ReframeTheme.tokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.toggleable(
                draft.applied,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onChange(draft.copy(applied = it)) },
            ).testTag("vacancy.applied"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            Checkbox(draft.applied, null, enabled = enabled)
            Text(stringResource(Res.string.vacancy_applied))
        }
        OutlinedTextField(
            draft.appliedDate,
            { onChange(draft.copy(appliedDate = it)) },
            Modifier.width(
                tokens.dimensions.documentDateWidth * APPLIED_FIELD_WIDTH_FACTOR,
            ).testTag("vacancy.applied.at"),
            enabled = enabled && draft.applied,
            label = { Text(stringResource(Res.string.vacancy_applied_at)) },
            supportingText = { Text(stringResource(Res.string.vacancy_applied_format)) },
            isError = !draft.validAppliedTime(),
            singleLine = true,
        )
        FillButton(
            stringResource(Res.string.vacancy_set_now),
            { onChange(draft.copy(appliedDate = appliedTimeText())) },
            Modifier.testTag("vacancy.applied.now"),
            enabled = enabled && draft.applied,
        )
    }
}

private const val APPLIED_FIELD_WIDTH_FACTOR = 2.5f
