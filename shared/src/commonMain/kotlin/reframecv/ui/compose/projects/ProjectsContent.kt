package reframecv.ui.compose.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_load_error
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.projects.editor.EditorContent
import reframecv.ui.compose.projects.empty.EmptyProjectContent
import reframecv.ui.compose.projects.list.NoProjectsContent
import reframecv.ui.compose.projects.list.ProjectListContent
import reframecv.ui.theme.ReframeTheme

internal const val PROJECTS_SCREEN_TAG = "projects.screen"

@Composable
fun ProjectsContent(component: ProjectsComponent) {
    val state by component.uiState.subscribeAsState()
    val editorSlot by component.editorSlot.subscribeAsState()
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .testTag(PROJECTS_SCREEN_TAG),
    ) {
        Box(Modifier.fillMaxSize().safeContentPadding()) {
            ProjectBody(state, component)
        }
        editorSlot.child?.instance?.let { EditorContent(it) }
    }
}

@Composable
private fun ProjectBody(state: UiState, component: ProjectsComponent) {
    when (state) {
        UiState.Loading -> LoadingContent(Modifier.fillMaxSize())

        UiState.NoProjects -> if (component.projectPath.isEmpty()) {
            NoProjectsContent(onAddProject = component::onAddProject)
        } else {
            EmptyProjectContent(
                onAddProject = component::onAddProject,
                onConfigureProject = component::onConfigureProject,
            )
        }

        is UiState.Projects -> ProjectListContent(
            projects = state.projects,
            onAddProject = component::onAddProject,
            onEditProject = component::onEditProject,
            onOpenProject = component::onOpenProject,
        )

        UiState.LoadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SelectableText(
                stringResource(Res.string.projects_load_error),
                modifier = Modifier.padding(ReframeTheme.tokens.spacing.medium),
                color = ReframeTheme.colorScheme.critical,
            )
        }
    }
}
