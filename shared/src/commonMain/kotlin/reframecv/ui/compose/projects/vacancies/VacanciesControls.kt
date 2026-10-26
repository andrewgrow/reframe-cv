package reframecv.ui.compose.projects.vacancies

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.vacancies_delete_selected
import reframecv.shared.generated.resources.vacancies_hide_applied
import reframecv.shared.generated.resources.vacancies_mark_applied
import reframecv.shared.generated.resources.vacancies_show_applied
import reframecv.shared.generated.resources.vacancy_add
import reframecv.ui.compose.common.DocumentListControls
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.VisibilityFilterIcon
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacanciesControls(
    onAdd: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onApplied: (() -> Unit)? = null,
    selectedCount: Int = 0,
    hideApplied: Boolean = false,
    onToggleAppliedFilter: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    DocumentListControls(
        showDestructive = onDelete != null,
        destructive = {
            onDelete?.let {
                FillButton(
                    stringResource(Res.string.vacancies_delete_selected, selectedCount),
                    it,
                    Modifier.testTag("vacancies.delete.selected"),
                    enabled = enabled && selectedCount > 0,
                    outlineState = FillButton.OutlineState.Visible,
                    fillColor = ReframeTheme.colorScheme.critical,
                    contentColor = ReframeTheme.colorScheme.critical,
                )
            }
        },
    ) {
        onToggleAppliedFilter?.let { toggle ->
            VisibilityFilterIcon(
                Icons.Default.CheckCircle,
                stringResource(
                    if (hideApplied) {
                        Res.string.vacancies_show_applied
                    } else {
                        Res.string.vacancies_hide_applied
                    },
                ),
                hideApplied,
                enabled,
                "vacancies.filter.applied",
                toggle,
            )
        }
        onApplied?.let { apply ->
            FillButton(
                stringResource(Res.string.vacancies_mark_applied),
                apply,
                Modifier.testTag("vacancies.applied.selected"),
                enabled = enabled && selectedCount > 0,
                outlineState = FillButton.OutlineState.Visible,
            )
        }
        FillButton(
            text = stringResource(Res.string.vacancy_add),
            onClick = onAdd,
            enabled = enabled,
            outlineState = FillButton.OutlineState.Visible,
        )
        onRetry?.let { retry ->
            FillButton(
                text = stringResource(Res.string.navigation_retry),
                onClick = retry,
                enabled = enabled,
                outlineState = FillButton.OutlineState.Visible,
            )
        }
    }
}
