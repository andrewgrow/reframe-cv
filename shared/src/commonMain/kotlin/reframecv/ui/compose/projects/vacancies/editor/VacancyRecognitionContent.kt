package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_parse_pdf
import reframecv.shared.generated.resources.vacancy_source_parsed
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyRecognitionContent(
    available: Boolean,
    enabled: Boolean,
    onRecognize: () -> Unit,
    parsed: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        TextButton(
            onRecognize,
            enabled = enabled && available,
            contentPadding = ReframeTheme.tokens.textButtonContentPadding,
        ) {
            Text(stringResource(Res.string.vacancy_parse_pdf))
        }
        if (parsed) {
            SelectableText(
                stringResource(Res.string.vacancy_source_parsed),
                color = ReframeTheme.colorScheme.success,
                style = ReframeTheme.tokens.typography.bodySmall,
            )
        }
    }
}
