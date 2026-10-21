package reframecv.ui.compose.projects.vacancies

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.vacancy_add
import reframecv.ui.compose.common.DocumentListControls
import reframecv.ui.compose.common.FillButton

@Composable
internal fun VacanciesControls(onAdd: () -> Unit, onRetry: (() -> Unit)? = null) {
    DocumentListControls {
        FillButton(
            text = stringResource(Res.string.vacancy_add),
            onClick = onAdd,
            outlineState = FillButton.OutlineState.Visible,
        )
        onRetry?.let { retry ->
            FillButton(
                text = stringResource(Res.string.navigation_retry),
                onClick = retry,
                outlineState = FillButton.OutlineState.Visible,
            )
        }
    }
}
