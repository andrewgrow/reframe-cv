package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.vacancy.Vacancy
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancies_select
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancySelectableRow(
    vacancy: Vacancy,
    timeZone: TimeZone,
    table: Boolean,
    selection: VacancyRowSelection,
    onEdit: () -> Unit,
) {
    val spacing = ReframeTheme.tokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        Row(
            Modifier.fillMaxWidth().testTag("vacancies.row.${vacancy.id}").clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = selection.enabled,
                role = Role.Button,
                onClick = onEdit,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            val label = stringResource(Res.string.vacancies_select, vacancy.name)
            Checkbox(
                selection.checked,
                { selection.toggle() },
                Modifier.size(ReframeTheme.tokens.dimensions.iconButtonSize)
                    .testTag("vacancies.select.${vacancy.id}").semantics {
                        contentDescription =
                            label
                    },
                enabled = selection.enabled,
            )
            Column(Modifier.weight(1f)) {
                VacancyRow(vacancy, timeZone, table) { if (selection.enabled) onEdit() }
            }
        }
        HorizontalDivider()
    }
}
