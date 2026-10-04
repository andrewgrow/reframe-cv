package reframecv.ui.compose.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import reframecv.domain.models.project.Project
import reframecv.ui.theme.ReframeTheme

internal const val PROJECTS_LIST_TAG = "projects.list"

@Composable
internal fun ProjectList(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium)
            .testTag(PROJECTS_LIST_TAG),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        items(projects, key = { it.id }) { project ->
            Text(
                project.name,
                modifier = Modifier.clickable { onProjectClick(project) },
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
