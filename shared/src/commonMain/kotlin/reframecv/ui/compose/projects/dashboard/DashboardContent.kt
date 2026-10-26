package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.dashboard_cover_letters
import reframecv.shared.generated.resources.dashboard_resumes
import reframecv.shared.generated.resources.dashboard_search_runners
import reframecv.shared.generated.resources.dashboard_title
import reframecv.shared.generated.resources.dashboard_vacancies
import reframecv.ui.components.projects.dashboard.DashboardComponent
import reframecv.ui.components.projects.dashboard.DashboardState
import reframecv.ui.compose.common.DocumentListControls
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.compose.settings.runners.RunnerEditorContent
import reframecv.ui.compose.settings.runners.RunnerRunsContent
import reframecv.ui.theme.ReframeTheme

internal const val DASHBOARD_TAG = "project.dashboard"
private const val SECTION_COUNT = 3

@Composable
fun DashboardContent(component: DashboardComponent) {
    val creationSlot by component.runners.creation.editor.subscribeAsState()
    val creation by component.runners.creation.state.subscribeAsState()
    creationSlot.child?.instance?.let {
        RunnerEditorContent(it)
        return
    }
    val slot by component.runners.execution.subscribeAsState()
    val picker by component.runners.state.subscribeAsState()
    val run = slot.child?.instance
    if (run != null) {
        RunnerRunsContent(run)
        return
    }
    val state by component.uiState.subscribeAsState()
    key(component.projectId) {
        Column(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.background,
            ).testTag(
                DASHBOARD_TAG,
            ).safeContentPadding().pageBackHandler(
                component::onBack,
                enabled =
                    !picker.visible && !creation.visible,
            ),
        ) {
            Box(Modifier.fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium)) {
                PageHeader(stringResource(Res.string.dashboard_title), component::onBack)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val current = state) {
                    DashboardState.Loading -> LoadingContent()

                    DashboardState.LoadFailed -> DashboardLoadError(component::onRetry)

                    is DashboardState.Ready -> DashboardLayout(
                        current,
                        component::onOpenVacancies,
                        component::onOpenResumes,
                        component::onOpenCoverLetters,
                    )
                }
            }
            HorizontalDivider()
            DocumentListControls {
                FillButton(
                    stringResource(Res.string.dashboard_search_runners),
                    component.runners::open,
                    Modifier.testTag("dashboard.search.runners"),
                    outlineState = FillButton.OutlineState.Visible,
                )
            }
            DashboardRunnerPicker(component.runners)
            DashboardRunnerCreationDialog(component.runners.creation)
        }
    }
}

@Composable
private fun DashboardLayout(
    state: DashboardState.Ready,
    onOpenVacancies: () -> Unit,
    onOpenResumes: () -> Unit,
    onOpenCoverLetters: () -> Unit,
) {
    val tokens = ReframeTheme.tokens
    val colors = MaterialTheme.colorScheme
    val appColors = ReframeTheme.colorScheme
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .padding(
                start = tokens.spacing.medium,
                end = tokens.spacing.medium,
                bottom = tokens.spacing.medium,
            ),
    ) {
        val availableWidth = maxWidth - tokens.spacing.medium * (SECTION_COUNT - 1)
        val sectionWidth = (availableWidth / SECTION_COUNT)
            .coerceAtLeast(tokens.dimensions.dashboardSectionMinWidth)
        Row(
            Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        ) {
            DashboardSection(
                stringResource(Res.string.dashboard_resumes),
                state.resumes.map { DashboardEntry(it.id, it.name) },
                appColors.dashboardResumes,
                colors.onSurface,
                Modifier.width(sectionWidth).fillMaxHeight(),
                onClick = onOpenResumes,
            )
            DashboardSection(
                stringResource(Res.string.dashboard_vacancies),
                state.vacancies.map { DashboardEntry(it.id, it.name) },
                appColors.dashboardVacancies,
                colors.onSurface,
                Modifier.width(sectionWidth).fillMaxHeight(),
                onClick = onOpenVacancies,
            )
            DashboardSection(
                stringResource(Res.string.dashboard_cover_letters),
                state.coverLetters.map { DashboardEntry(it.id, it.name) },
                appColors.dashboardCoverLetters,
                colors.onSurface,
                Modifier.width(sectionWidth).fillMaxHeight(),
                onClick = onOpenCoverLetters,
            )
        }
    }
}
