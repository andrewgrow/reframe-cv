package reframecv.ui.compose.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.project.Project
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.projects_add
import reframecv.shared.generated.resources.projects_empty
import reframecv.shared.generated.resources.projects_help
import reframecv.shared.generated.resources.projects_help_hide
import reframecv.shared.generated.resources.projects_help_show
import reframecv.shared.generated.resources.projects_load_error
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.UiState
import reframecv.ui.components.projects.UiState.NoProjects
import reframecv.ui.compose.projects.editor.EditorContent
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
            NoProjects -> NoProjectsContent(onAddProject = component::onAddProject)

            is UiState.Projects -> ProjectListContent(
                projects = currentState.projects,
                projectPath = component.projectPath,
                onAddProject = component::onAddProject,
                onProjectClick = component::onProjectClick,
            )

            UiState.LoadFailed -> Text(
                stringResource(Res.string.projects_load_error),
                color = ReframeTheme.colorScheme.critical,
            )
        }

        editorSlot.child?.instance?.let { EditorContent(it) }
    }
}

@Composable
private fun NoProjectsContent(onAddProject: () -> Unit) {
    val tokens = ReframeTheme.tokens
    var showProjectHelp by remember { mutableStateOf(false) }
    val emptyMessage = stringResource(Res.string.projects_empty)
    val addProjectLabel = stringResource(Res.string.projects_add)
    val helpMessage = stringResource(Res.string.projects_help)
    val showHelpLabel = stringResource(Res.string.projects_help_show)
    val hideHelpLabel = stringResource(Res.string.projects_help_hide)

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(tokens.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = tokens.spacing.medium,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectableText(emptyMessage, color = MaterialTheme.colorScheme.onBackground)
                IconButton(
                    onClick = { showProjectHelp = !showProjectHelp },
                    modifier = Modifier.size(tokens.dimensions.iconButtonSize),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        modifier = Modifier.size(tokens.dimensions.iconSize),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = if (showProjectHelp) hideHelpLabel else showHelpLabel,
                    )
                }
            }
            AnimatedVisibility(
                visible = showProjectHelp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SelectableText(
                    text = helpMessage,
                    modifier = Modifier
                        .widthIn(max = tokens.dimensions.projectHelpMaxWidth)
                        .padding(top = tokens.spacing.medium),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReframeTheme.colorScheme.hint,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Button(onClick = onAddProject, contentPadding = tokens.buttonContentPadding) {
            Text(addProjectLabel)
        }
    }
}

@Composable
private fun ProjectListContent(
    projects: List<Project>,
    projectPath: List<ProjectBreadcrumb>,
    onAddProject: () -> Unit,
    onProjectClick: (Project) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding(),
    ) {
        ProjectBreadcrumbs(projectPath)
        HorizontalDivider()
        ProjectList(
            projects = projects,
            onProjectClick = onProjectClick,
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
