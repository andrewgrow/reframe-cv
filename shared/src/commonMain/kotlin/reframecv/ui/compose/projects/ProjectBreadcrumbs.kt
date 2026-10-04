package reframecv.ui.compose.projects

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_projects_list
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.Spacing

@Composable
internal fun ProjectBreadcrumbs(
    projectPath: List<ProjectBreadcrumb>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(projectPath, scrollState.maxValue) {
        if (scrollState.maxValue != Int.MAX_VALUE) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(Spacing.medium)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val rootName = stringResource(Res.string.navigation_projects_list)
        val breadcrumbs = listOf(rootName) + projectPath.map { it.name }
        breadcrumbs.forEachIndexed { index, name ->
            if (index > 0) {
                Text(">", color = ReframeTheme.colorScheme.hint)
            }
            SelectableText(name, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
