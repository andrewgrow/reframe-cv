package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_import_error
import reframecv.shared.generated.resources.letter_import_progress
import reframecv.shared.generated.resources.letter_import_success
import reframecv.ui.components.projects.coverletters.CoverLetterImportComponent
import reframecv.ui.components.projects.coverletters.CoverLetterImportState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun CoverLetterImportFeedback(component: CoverLetterImportComponent) {
    val state by component.state.subscribeAsState()
    if (state == CoverLetterImportState.Idle) return
    val tokens = ReframeTheme.tokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        if (state == CoverLetterImportState.Importing) {
            CircularProgressIndicator(
                Modifier.size(tokens.spacing.medium).testTag("cover_letter.import.progress"),
                strokeWidth = tokens.outlineWidth,
            )
        }
        SelectableText(
            stringResource(
                when (state) {
                    CoverLetterImportState.Importing -> Res.string.letter_import_progress
                    CoverLetterImportState.Imported -> Res.string.letter_import_success
                    else -> Res.string.letter_import_error
                },
            ),
            color = when (state) {
                CoverLetterImportState.Failed -> ReframeTheme.colorScheme.critical
                CoverLetterImportState.Imported -> ReframeTheme.colorScheme.success
                else -> ReframeTheme.colorScheme.hint
            },
            style = tokens.typography.bodySmall,
        )
    }
}
