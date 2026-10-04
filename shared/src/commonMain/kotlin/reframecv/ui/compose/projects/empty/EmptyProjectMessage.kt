package reframecv.ui.compose.projects.empty

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.project_empty
import reframecv.shared.generated.resources.projects_help_hide
import reframecv.shared.generated.resources.projects_help_show
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun EmptyProjectMessage(showHelp: Boolean, onToggleHelp: () -> Unit) {
    val tokens = ReframeTheme.tokens
    val emptyMessage = stringResource(Res.string.project_empty)
    val showHelpLabel = stringResource(Res.string.projects_help_show)
    val hideHelpLabel = stringResource(Res.string.projects_help_hide)
    Row(verticalAlignment = Alignment.CenterVertically) {
        SelectableText(
            emptyMessage,
            selectionModifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = onToggleHelp,
            modifier = Modifier.size(tokens.dimensions.iconButtonSize),
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = if (showHelp) hideHelpLabel else showHelpLabel,
                modifier = Modifier.size(tokens.dimensions.iconSize),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
