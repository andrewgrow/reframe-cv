package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import kotlinx.datetime.TimeZone
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.theme.ReframeTheme

internal const val VACANCIES_LIST_TAG = "vacancies.list"

@Composable
internal fun VacanciesList(
    vacancies: List<Vacancy>,
    timeZone: TimeZone,
    onEdit: (Vacancy) -> Unit,
) {
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
                        modifier = Modifier.fillMaxWidth().testTag("vacancies.row.${vacancy.id}")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                            ) { onEdit(vacancy) },
                        verticalArrangement = Arrangement.spacedBy(
                            ReframeTheme.tokens.spacing.medium,
                        ),
                    ) {
                        VacancyRow(vacancy, timeZone, table) { onEdit(vacancy) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
