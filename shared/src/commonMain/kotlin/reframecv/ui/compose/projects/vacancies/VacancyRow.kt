package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.vacancy.Vacancy
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_company_missing
import reframecv.ui.compose.common.documentDate
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyRow(vacancy: Vacancy, timeZone: TimeZone, table: Boolean, onEdit: () -> Unit) {
    val company = vacancy.company.ifBlank { stringResource(Res.string.vacancy_company_missing) }
    val date = documentDate(vacancy.createdAt, timeZone)
    if (table) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        ) {
            SelectableText(
                vacancy.name,
                modifier = vacancyTextClick(onEdit),
                selectionModifier = Modifier.weight(VACANCY_NAME_WEIGHT),
                style = MaterialTheme.typography.bodyLarge,
            )
            SelectableText(
                company,
                modifier = vacancyTextClick(onEdit),
                selectionModifier = Modifier.weight(VACANCY_COMPANY_WEIGHT),
                style = MaterialTheme.typography.bodyMedium,
            )
            VacancyAppliedDate(
                vacancy,
                timeZone,
                Modifier.width(ReframeTheme.tokens.dimensions.documentDateWidth),
            )
            SelectableText(
                date,
                modifier = vacancyTextClick(onEdit),
                selectionModifier = Modifier.width(
                    ReframeTheme.tokens.dimensions.documentDateWidth,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
            SelectableText(
                vacancy.name,
                modifier = vacancyTextClick(onEdit),
                style = MaterialTheme.typography.bodyLarge,
            )
            SelectableText(
                company,
                modifier = vacancyTextClick(onEdit),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (vacancy.appliedAt != null) VacancyAppliedDate(vacancy, timeZone)
            SelectableText(
                date,
                modifier = vacancyTextClick(onEdit),
                color = ReframeTheme.colorScheme.hint,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun vacancyTextClick(onEdit: () -> Unit): Modifier = Modifier.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    role = Role.Button,
    onClick = onEdit,
)

@Composable
private fun VacancyAppliedDate(
    vacancy: Vacancy,
    timeZone: TimeZone,
    modifier: Modifier = Modifier,
) {
    Text(
        vacancy.appliedAt?.let { "✓ " + documentDate(it, timeZone) }.orEmpty(),
        modifier.testTag("vacancies.applied.${vacancy.id}"),
        color = ReframeTheme.colorScheme.success,
        style = MaterialTheme.typography.bodyMedium,
    )
}
