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

@Composable
internal fun VacancyTableHeader() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        SelectableText(
            stringResource(Res.string.vacancy_added),
            selectionModifier = Modifier.width(ReframeTheme.tokens.dimensions.vacancyDateWidth),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.vacancy_company),
            selectionModifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.vacancy_title),
            selectionModifier = Modifier.weight(2f),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
