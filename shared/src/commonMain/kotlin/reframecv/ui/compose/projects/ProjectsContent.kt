package reframecv.ui.compose.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_load_error
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.components.projects.UiState.NoProjects
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.projects.common.ProjectBreadcrumbs
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
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PROJECTS_SCREEN_TAG),
    ) {
        when (val currentState = state) {
            UiState.Loading -> Column(Modifier.fillMaxSize().safeContentPadding()) {
                if (component.projectPath.isNotEmpty()) {
                    ProjectBreadcrumbs(
                        component.projectPath,
                        onBreadcrumb = component::onBreadcrumb,
                    )
                    HorizontalDivider()
                }
                LoadingContent(Modifier.weight(1f))
            }

            NoProjects -> if (component.projectPath.isEmpty()) {
                NoProjectsContent(onAddProject = component::onAddProject)
            } else {
                Column(Modifier.fillMaxSize().safeContentPadding()) {
                    ProjectBreadcrumbs(
                        component.projectPath,
                        onBreadcrumb = component::onBreadcrumb,
                    )
                    HorizontalDivider()
                    EmptyProjectContent(onAddProject = component::onAddProject)
                }
            }

            is UiState.Projects -> ProjectListContent(
                projects = currentState.projects,
                projectPath = component.projectPath,
                onAddProject = component::onAddProject,
                onEditProject = component::onEditProject,
                onOpenProject = component::onOpenProject,
                onBreadcrumb = component::onBreadcrumb,
            )

            UiState.LoadFailed -> Text(
                stringResource(Res.string.projects_load_error),
                color = ReframeTheme.colorScheme.critical,
            )
        }

        editorSlot.child?.instance?.let { EditorContent(it) }
    }
}
