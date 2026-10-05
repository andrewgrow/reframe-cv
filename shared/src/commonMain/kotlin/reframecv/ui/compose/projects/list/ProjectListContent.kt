package reframecv.ui.compose.projects.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.project.Project
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_add

@Composable
internal fun ProjectListContent(
    projects: List<Project>,
    onAddProject: () -> Unit,
    onEditProject: (Project) -> Unit,
    onOpenProject: (Project) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
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
