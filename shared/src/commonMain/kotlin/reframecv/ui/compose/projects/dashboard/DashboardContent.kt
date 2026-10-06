package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import reframecv.shared.generated.resources.dashboard_vacancies
import reframecv.ui.components.projects.dashboard.DashboardComponent
import reframecv.ui.components.projects.dashboard.DashboardState
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.theme.ReframeTheme

internal const val DASHBOARD_TAG = "project.dashboard"
private const val SECTION_COUNT = 3

@Composable
fun DashboardContent(component: DashboardComponent) {
    val state by component.uiState.subscribeAsState()
    key(component.projectId) {
        Box(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.background,
            ).testTag(DASHBOARD_TAG),
        ) {
            when (val current = state) {
                DashboardState.Loading -> LoadingContent()
                DashboardState.LoadFailed -> DashboardLoadError(component::onRetry)
                is DashboardState.Ready -> DashboardLayout(current)
            }
        }
    }
}

@Composable
private fun DashboardLayout(state: DashboardState.Ready) {
    val tokens = ReframeTheme.tokens
    val colors = MaterialTheme.colorScheme
    val appColors = ReframeTheme.colorScheme
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .safeContentPadding().padding(tokens.spacing.medium),
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
            )
            DashboardSection(
                stringResource(Res.string.dashboard_vacancies),
                state.vacancies.map { DashboardEntry(it.id, it.name) },
                appColors.dashboardVacancies,
                colors.onSurface,
                Modifier.width(sectionWidth).fillMaxHeight(),
            )
            DashboardSection(
                stringResource(Res.string.dashboard_cover_letters),
                state.coverLetters.map { DashboardEntry(it.id, it.name) },
                appColors.dashboardCoverLetters,
                colors.onSurface,
                Modifier.width(sectionWidth).fillMaxHeight(),
            )
        }
    }
}
