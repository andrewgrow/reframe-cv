package reframecv.ui.compose.projects.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_add
import reframecv.shared.generated.resources.projects_empty
import reframecv.shared.generated.resources.projects_help
import reframecv.shared.generated.resources.projects_help_hide
import reframecv.shared.generated.resources.projects_help_show
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun NoProjectsContent(onAddProject: () -> Unit) {
    val tokens = ReframeTheme.tokens
    var showProjectHelp by remember { mutableStateOf(false) }
    val emptyMessage = stringResource(Res.string.projects_empty)
    val addProjectLabel = stringResource(Res.string.projects_add)
    val helpMessage = stringResource(Res.string.projects_help)
    val showHelpLabel = stringResource(Res.string.projects_help_show)
    val hideHelpLabel = stringResource(Res.string.projects_help_hide)

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(tokens.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = tokens.spacing.medium,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectableText(emptyMessage, color = MaterialTheme.colorScheme.onBackground)
                IconButton(
                    onClick = { showProjectHelp = !showProjectHelp },
                    modifier = Modifier.size(tokens.dimensions.iconButtonSize),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        modifier = Modifier.size(tokens.dimensions.iconSize),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = if (showProjectHelp) hideHelpLabel else showHelpLabel,
                    )
                }
            }
            AnimatedVisibility(
                visible = showProjectHelp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SelectableText(
                    text = helpMessage,
                    modifier = Modifier
                        .widthIn(max = tokens.dimensions.projectHelpMaxWidth)
                        .padding(top = tokens.spacing.medium),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReframeTheme.colorScheme.hint,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Button(onClick = onAddProject, contentPadding = tokens.buttonContentPadding) {
            Text(addProjectLabel)
        }
    }
}
