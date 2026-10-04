package reframecv.ui.compose.projects.empty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_empty_help
import reframecv.shared.generated.resources.project_empty_help_example
import reframecv.shared.generated.resources.project_empty_help_footer
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun EmptyProjectHelp() {
    val message = stringResource(Res.string.project_empty_help)
    val example = stringResource(Res.string.project_empty_help_example)
    val footer = stringResource(Res.string.project_empty_help_footer)
    Column(
        modifier = Modifier.padding(top = ReframeTheme.tokens.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        SelectableText(
            message,
            color = ReframeTheme.colorScheme.hint,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        SelectableText(
            example,
            color = ReframeTheme.colorScheme.hint,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
        SelectableText(
            footer,
            color = ReframeTheme.colorScheme.hint,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
