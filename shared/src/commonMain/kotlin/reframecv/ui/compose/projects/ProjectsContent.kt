package reframecv.ui.compose.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_add
import reframecv.shared.generated.resources.projects_empty
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.UiState.NoProjects
import reframecv.ui.compose.projects.editor.EditorContent
import reframecv.ui.theme.Spacing

internal const val PROJECTS_SCREEN_TAG = "projects.screen"

@Composable
fun ProjectsContent(component: ProjectsComponent) {
    val state by component.uiState.subscribeAsState()
    val editorSlot by component.editorSlot.subscribeAsState()

    Box(Modifier.fillMaxSize().testTag(PROJECTS_SCREEN_TAG)) {
        when (state) {
            NoProjects -> NoProjectsContent(onAddProject = component::onAddProject)
        }

        editorSlot.child?.instance?.let { EditorContent(it) }
    }
}

@Composable
private fun NoProjectsContent(onAddProject: () -> Unit) {
    val emptyMessage = stringResource(Res.string.projects_empty)
    val addProjectLabel = stringResource(Res.string.projects_add)

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = Spacing.medium,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Text(emptyMessage)
        Button(onClick = onAddProject) {
            Text(addProjectLabel)
        }
    }
}
