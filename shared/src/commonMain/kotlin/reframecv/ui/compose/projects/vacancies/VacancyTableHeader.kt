package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_added
import reframecv.shared.generated.resources.vacancy_company
import reframecv.shared.generated.resources.vacancy_title
import reframecv.ui.theme.ReframeTheme

internal const val VACANCY_TITLE_WEIGHT = 1f
internal const val VACANCY_COMPANY_WEIGHT = 3f

@Composable
internal fun VacancyTableHeader() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        SelectableText(
            stringResource(Res.string.vacancy_title),
            selectionModifier = Modifier.weight(VACANCY_TITLE_WEIGHT),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.vacancy_company),
            selectionModifier = Modifier.weight(VACANCY_COMPANY_WEIGHT),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.vacancy_added),
            selectionModifier = Modifier.width(ReframeTheme.tokens.dimensions.vacancyDateWidth),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
