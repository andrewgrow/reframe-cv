package reframecv.ui.compose.projects.vacancies

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancies_load_error
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacanciesLoadError() {
    SelectableText(
        stringResource(Res.string.vacancies_load_error),
        color = ReframeTheme.colorScheme.critical,
        style = MaterialTheme.typography.bodyLarge,
    )
}
