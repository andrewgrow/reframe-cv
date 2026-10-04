package reframecv.ui.compose.projects.empty

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun EmptyProjectContent(onAddProject: () -> Unit) {
    val tokens = ReframeTheme.tokens
    var showHelp by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(tokens.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            tokens.spacing.medium,
            Alignment.CenterVertically,
        ),
    ) {
        Column(
            modifier = Modifier.widthIn(max = tokens.dimensions.projectHelpMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyProjectMessage(showHelp = showHelp, onToggleHelp = { showHelp = !showHelp })
            AnimatedVisibility(
                visible = showHelp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                EmptyProjectHelp()
            }
        }
        EmptyProjectActions(onAddProject)
    }
}
