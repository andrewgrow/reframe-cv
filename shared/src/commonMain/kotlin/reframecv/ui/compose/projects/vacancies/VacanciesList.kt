package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.datetime.TimeZone
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.theme.ReframeTheme

internal const val VACANCIES_LIST_TAG = "vacancies.list"

@Composable
internal fun VacanciesList(vacancies: List<Vacancy>, timeZone: TimeZone) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val table = maxWidth >= ReframeTheme.tokens.dimensions.vacancyTableMinWidth
        Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
            if (table) VacancyTableHeader()
            LazyColumn(
                Modifier.weight(1f).testTag(VACANCIES_LIST_TAG),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                items(vacancies, key = { it.id }) { vacancy ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            ReframeTheme.tokens.spacing.medium,
                        ),
                    ) {
                        VacancyRow(vacancy, timeZone, table)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
