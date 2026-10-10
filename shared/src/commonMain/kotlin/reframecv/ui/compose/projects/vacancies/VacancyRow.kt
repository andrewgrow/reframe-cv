package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.vacancy.Vacancy
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_company_missing
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyRow(vacancy: Vacancy, timeZone: TimeZone, table: Boolean) {
    val company = vacancy.company.ifBlank { stringResource(Res.string.vacancy_company_missing) }
    val date = vacancyDate(vacancy.createdAt, timeZone)
    if (table) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        ) {
            SelectableText(
                date,
                selectionModifier = Modifier.width(ReframeTheme.tokens.dimensions.vacancyDateWidth),
                style = MaterialTheme.typography.bodyMedium,
            )
            SelectableText(
                company,
                selectionModifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            SelectableText(
                vacancy.name,
                selectionModifier = Modifier.weight(2f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
            SelectableText(vacancy.name, style = MaterialTheme.typography.bodyLarge)
            SelectableText(company, style = MaterialTheme.typography.bodyMedium)
            SelectableText(
                date,
                color = ReframeTheme.colorScheme.hint,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
