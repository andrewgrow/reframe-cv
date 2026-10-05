package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.dashboard_cover_letters
import reframecv.shared.generated.resources.dashboard_resumes
import reframecv.shared.generated.resources.dashboard_vacancies
import reframecv.ui.components.projects.dashboard.DashboardComponent
import reframecv.ui.theme.ReframeTheme

internal const val DASHBOARD_TAG = "project.dashboard"
private const val SECTION_COUNT = 3

@Composable
fun DashboardContent(component: DashboardComponent) {
    key(component.projectId) { DashboardLayout() }
}

@Composable
private fun DashboardLayout() {
    val tokens = ReframeTheme.tokens
    val colors = MaterialTheme.colorScheme
    val appColors = ReframeTheme.colorScheme
    BoxWithConstraints(
        Modifier.fillMaxSize().background(colors.background).testTag(DASHBOARD_TAG)
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
                appColors.dashboardResumes,
                colors.onSurface,
                Modifier.width(sectionWidth).fillMaxHeight(),
            )
            DashboardSection(
                stringResource(Res.string.dashboard_vacancies),
                appColors.dashboardVacancies,
                colors.onSurface,
                Modifier.width(sectionWidth).fillMaxHeight(),
            )
            DashboardSection(
                stringResource(Res.string.dashboard_cover_letters),
                appColors.dashboardCoverLetters,
                colors.onSurface,
                Modifier.width(sectionWidth).fillMaxHeight(),
            )
        }
    }
}
