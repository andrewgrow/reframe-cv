package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_attach_snapshot
import reframecv.shared.generated.resources.vacancy_snapshot_hint
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancySnapshotPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        TextButton(
            onClick = {},
            enabled = false,
            contentPadding = ReframeTheme.tokens.textButtonContentPadding,
        ) {
            Text(stringResource(Res.string.vacancy_attach_snapshot))
        }
        SelectableText(
            stringResource(Res.string.vacancy_snapshot_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
    }
}
