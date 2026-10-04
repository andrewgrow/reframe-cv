package reframecv.ui.compose.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_edit
import reframecv.ui.theme.ReframeTheme

internal fun projectRowTag(id: Long): String = "projects.row.$id"

@Composable
internal fun ProjectRow(project: Project, onEditProject: (Project) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().testTag(projectRowTag(project.id))
            .onFocusChanged { focused = it.hasFocus }
            .focusable(interactionSource = interactionSource)
            .hoverable(interactionSource),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            project.name,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
        )
        AnimatedVisibility(visible = hovered || focused, enter = fadeIn(), exit = fadeOut()) {
            Text(
                stringResource(Res.string.action_edit),
                modifier = Modifier.clickable(role = Role.Button) { onEditProject(project) },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
