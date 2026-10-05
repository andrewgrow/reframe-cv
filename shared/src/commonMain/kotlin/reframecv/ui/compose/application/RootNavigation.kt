package reframecv.ui.compose.application

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_projects
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.projects_load_error
import reframecv.ui.components.application.navigation.ProjectTreeComponent
import reframecv.ui.components.application.navigation.ProjectTreeRow
import reframecv.ui.components.application.navigation.visibleRows
import reframecv.ui.compose.application.navigation.ProjectTreeEntry
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RootNavigation(component: ProjectTreeComponent, modifier: Modifier = Modifier) {
    val state by component.state.subscribeAsState()
    val rows = state.visibleRows()
    val tokens = ReframeTheme.tokens
    val listState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()
    RevealTreeSelection(state.selectedId, rows, listState, horizontalScroll)
    Box(
        modifier.width(tokens.dimensions.navigationWidth).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag("navigation.tree"),
    ) {
        val contentWidth = (
            tokens.dimensions.projectTreeTextMinWidth + tokens.dimensions.iconSize +
                tokens.spacing.small * 3 +
                tokens.spacing.medium * (rows.maxOfOrNull { it.depth } ?: 0)
            ).coerceAtLeast(tokens.dimensions.navigationWidth)
        Box(Modifier.horizontalScroll(horizontalScroll)) {
            LazyColumn(Modifier.width(contentWidth).fillMaxHeight(), state = listState) {
                item(key = "root") {
                    ProjectTreeEntry(
                        name = stringResource(Res.string.navigation_projects),
                        selected = state.selectedId == null,
                        onClick = component::onProjectsList,
                        modifier = Modifier.testTag("navigation.root"),
                    )
                }
                items(rows, key = { it.project.id }) { row ->
                    ProjectTreeEntry(
                        name = row.project.name,
                        selected = state.selectedId == row.project.id,
                        depth = row.depth,
                        hasChildren = row.hasChildren,
                        expanded = row.project.id in state.expandedIds,
                        onToggle = { component.onToggle(row.project.id) },
                        onClick = { component.onProjectSelected(row.project.id) },
                        modifier = Modifier.testTag("navigation.project.${row.project.id}"),
                    )
                }
                if (state.failed) {
                    item(key = "error") {
                        NavigationLoadError(component::onRetry)
                    }
                }
            }
        }
        if (state.loading) LoadingContent(Modifier.fillMaxWidth().fillMaxHeight())
    }
}

@Composable
private fun NavigationLoadError(onRetry: () -> Unit) {
    Text(
        stringResource(Res.string.projects_load_error),
        modifier = Modifier.fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
        color = ReframeTheme.colorScheme.critical,
        style = MaterialTheme.typography.bodySmall,
    )
    ProjectTreeEntry(
        name = stringResource(Res.string.navigation_retry),
        selected = false,
        onClick = onRetry,
    )
}

@Composable
private fun RevealTreeSelection(
    selectedId: Long?,
    rows: List<ProjectTreeRow>,
    listState: LazyListState,
    horizontalScroll: ScrollState,
) {
    val indent = with(LocalDensity.current) { ReframeTheme.tokens.spacing.medium.roundToPx() }
    LaunchedEffect(selectedId, rows.map { it.project.id }) {
        val index = rows.indexOfFirst { it.project.id == selectedId }
        if (index >= 0) {
            listState.animateScrollToItem(index + 1)
            horizontalScroll.animateScrollTo(rows[index].depth * indent)
        } else if (selectedId == null) {
            listState.animateScrollToItem(0)
            horizontalScroll.animateScrollTo(0)
        }
    }
}
