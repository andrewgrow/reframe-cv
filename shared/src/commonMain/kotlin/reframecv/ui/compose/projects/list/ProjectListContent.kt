package reframecv.ui.compose.projects.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_add
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.compose.projects.common.ProjectBreadcrumbs

@Composable
internal fun ProjectListContent(
    projects: List<Project>,
    projectPath: List<ProjectBreadcrumb>,
    onAddProject: () -> Unit,
    onEditProject: (Project) -> Unit,
    onOpenProject: (Project) -> Unit,
    onBreadcrumb: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding(),
    ) {
        ProjectBreadcrumbs(projectPath, onBreadcrumb = onBreadcrumb)
        HorizontalDivider()
        ProjectList(
            projects = projects,
            onEditProject = onEditProject,
            onOpenProject = onOpenProject,
            modifier = Modifier.weight(1f),
        )
        HorizontalDivider()
        ProjectControls(
            actions = listOf(
                ProjectAction(
                    label = stringResource(Res.string.projects_add),
                    onClick = onAddProject,
                ),
            ),
        )
    }
}
