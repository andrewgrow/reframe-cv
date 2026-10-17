package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_import_error
import reframecv.shared.generated.resources.vacancy_import_progress
import reframecv.shared.generated.resources.vacancy_import_success
import reframecv.ui.components.projects.vacancies.editor.VacancyImportState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyImportFeedback(state: VacancyImportState) {
    if (state == VacancyImportState.Idle) return
    val tokens = ReframeTheme.tokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        if (state == VacancyImportState.Importing) {
            CircularProgressIndicator(
                Modifier.size(tokens.spacing.medium).testTag("vacancy.import.progress"),
                strokeWidth = tokens.outlineWidth,
            )
        }
        SelectableText(
            stringResource(
                when (state) {
                    VacancyImportState.Importing -> Res.string.vacancy_import_progress
                    VacancyImportState.Imported -> Res.string.vacancy_import_success
                    else -> Res.string.vacancy_import_error
                },
            ),
            color = when (state) {
                VacancyImportState.Failed -> ReframeTheme.colorScheme.critical
                VacancyImportState.Imported -> ReframeTheme.colorScheme.success
                else -> ReframeTheme.colorScheme.hint
            },
            style = tokens.typography.bodySmall,
        )
    }
}
