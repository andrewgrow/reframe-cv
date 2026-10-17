package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.recognition.RecognitionError
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_parse_cancel
import reframecv.shared.generated.resources.vacancy_parse_changed
import reframecv.shared.generated.resources.vacancy_parse_invalid_pdf
import reframecv.shared.generated.resources.vacancy_parse_invalid_response
import reframecv.shared.generated.resources.vacancy_parse_not_configured
import reframecv.shared.generated.resources.vacancy_parse_not_recognized
import reframecv.shared.generated.resources.vacancy_parse_pdf
import reframecv.shared.generated.resources.vacancy_parse_progress
import reframecv.shared.generated.resources.vacancy_parse_success
import reframecv.shared.generated.resources.vacancy_parse_unavailable
import reframecv.shared.generated.resources.vacancy_parse_unreadable
import reframecv.shared.generated.resources.vacancy_parse_unsupported
import reframecv.ui.components.projects.vacancies.editor.VacancyRecognitionComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyRecognitionState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyRecognitionContent(
    component: VacancyRecognitionComponent,
    available: Boolean,
    enabled: Boolean,
    onRecognize: () -> Unit,
) {
    val state by component.state.subscribeAsState()
    val processing = state == VacancyRecognitionState.Processing
    val tokens = ReframeTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (processing) {
                CircularProgressIndicator(
                    Modifier.size(tokens.spacing.medium).testTag("vacancy.parse.progress"),
                    strokeWidth = tokens.outlineWidth,
                )
                TextButton(component::onCancel, contentPadding = tokens.textButtonContentPadding) {
                    Text(stringResource(Res.string.vacancy_parse_cancel))
                }
            } else {
                TextButton(
                    onRecognize,
                    enabled = enabled && available,
                    contentPadding = tokens.textButtonContentPadding,
                ) {
                    Text(stringResource(Res.string.vacancy_parse_pdf))
                }
            }
        }
        RecognitionFeedback(state)
    }
}

@Composable
private fun RecognitionFeedback(state: VacancyRecognitionState) {
    val resource = when (state) {
        VacancyRecognitionState.Idle -> return
        VacancyRecognitionState.Processing -> Res.string.vacancy_parse_progress
        VacancyRecognitionState.Applied -> Res.string.vacancy_parse_success
        is VacancyRecognitionState.Failed -> recognitionErrorResource(state.error)
    }
    val tokens = ReframeTheme.tokens
    val color = when (state) {
        is VacancyRecognitionState.Failed -> ReframeTheme.colorScheme.critical
        VacancyRecognitionState.Applied -> ReframeTheme.colorScheme.success
        else -> ReframeTheme.colorScheme.hint
    }
    SelectableText(stringResource(resource), style = tokens.typography.bodySmall, color = color)
    if (state is VacancyRecognitionState.Failed && !state.details.isNullOrBlank()) {
        SelectableText(state.details, style = tokens.typography.bodySmall, color = color)
    }
}

private fun recognitionErrorResource(error: RecognitionError) = when (error) {
    RecognitionError.NotConfigured -> Res.string.vacancy_parse_not_configured
    RecognitionError.UnreadableFile -> Res.string.vacancy_parse_unreadable
    RecognitionError.UnsupportedFile -> Res.string.vacancy_parse_unsupported
    RecognitionError.InvalidPdf -> Res.string.vacancy_parse_invalid_pdf
    RecognitionError.ChangedFile -> Res.string.vacancy_parse_changed
    RecognitionError.NotRecognized -> Res.string.vacancy_parse_not_recognized
    RecognitionError.ServiceUnavailable -> Res.string.vacancy_parse_unavailable
    RecognitionError.InvalidResponse -> Res.string.vacancy_parse_invalid_response
}
