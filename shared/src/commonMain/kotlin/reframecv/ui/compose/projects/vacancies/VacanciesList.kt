package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.vacancy.Vacancy
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancies_select
import reframecv.shared.generated.resources.vacancies_select_all
import reframecv.ui.components.projects.vacancies.VacancySelectionComponent
import reframecv.ui.theme.ReframeTheme

internal const val VACANCIES_LIST_TAG = "vacancies.list"

@Composable
internal fun VacanciesList(
    vacancies: List<Vacancy>,
    timeZone: TimeZone,
    onEdit: (Vacancy) -> Unit,
    selection: VacancySelectionComponent,
) {
    val selected by selection.state.subscribeAsState()
    val enabled = !selected.confirming && !selected.deleting
    val toggleState = when {
        selected.selected.isEmpty() -> ToggleableState.Off
        vacancies.all { it.id in selected.selected } -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val table = maxWidth >= ReframeTheme.tokens.dimensions.vacancyTableMinWidth
        Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
            if (table) {
                VacancyTableHeader(toggleState, enabled, selection::onToggleAll)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VacancySelectAll(toggleState, enabled, selection::onToggleAll)
                    Text(stringResource(Res.string.vacancies_select_all))
                }
            }
            LazyColumn(
                Modifier.weight(1f).testTag(VACANCIES_LIST_TAG),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                items(vacancies, key = { it.id }) { vacancy ->
                    VacancySelectableRow(
                        vacancy,
                        timeZone,
                        table,
                        VacancyRowSelection(vacancy.id in selected.selected, enabled) {
                            selection.onToggle(vacancy.id)
                        },
                    ) { onEdit(vacancy) }
                }
            }
        }
    }
}
