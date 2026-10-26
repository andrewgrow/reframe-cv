package reframecv.ui.compose.projects.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.dashboard.DashboardRunnerCreationState
import reframecv.ui.components.projects.dashboard.DashboardRunnersState
import reframecv.ui.components.projects.dashboard.TestDashboardComponent
import reframecv.ui.components.projects.dashboard.TestDashboardRunnerCreationComponent
import reframecv.ui.components.projects.dashboard.TestDashboardRunnersComponent
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.usecase.SavedRunner

@OptIn(ExperimentalTestApi::class)
class DashboardRunnerPickerScreenshotTest : GoldenScreenshotTest() {
    private val owner =
        ProviderConnection(
            id = 1,
            type = ApiProviderType.Apify,
            name = "Example connection",
            configuration = ApifyConfiguration("fictional-token"),
        )
    private val runner = RunnerDraft(
        name = "Kotlin search",
        searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin",
    )
        .applyTo(RunnerDraft.newRunner(1)).copy(id = 1)

    @Test
    fun pickerShowsNamesConnectionsAndDisabledStatus() = runDesktopComposeUiTest {
        val picker = TestDashboardRunnersComponent(
            DashboardRunnersState(
                runners = listOf(
                    SavedRunner(runner, owner),
                    SavedRunner(
                        runner.copy(id = 2, name = "", active = false),
                        owner.copy(name = ""),
                    ),
                ),
            ),
        )
        setGoldenContent(this) { DashboardContent(TestDashboardComponent(runners = picker)) }
        onNodeWithTag("dashboard.search.runners").performClick()
        captureGolden(this, "dashboard.runners.dialog")
        onNodeWithTag("dashboard.runner.1").performClick()
        assertEquals(1L, picker.selected)
    }

    @Test
    fun emptyPickerExplainsWhereToAddRunners() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = "dashboard.runners.dialog") {
            DashboardContent(
                TestDashboardComponent(
                    runners = TestDashboardRunnersComponent(DashboardRunnersState(visible = true)),
                ),
            )
        }
    }

    @Test
    fun failedPickerOffersRetry() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = "dashboard.runners.dialog") {
            DashboardContent(
                TestDashboardComponent(
                    runners = TestDashboardRunnersComponent(
                        DashboardRunnersState(visible = true, failed = true),
                    ),
                ),
            )
        }
    }

    @Test
    fun addRunnerReplacesPickerWithConnectionSelection() = runDesktopComposeUiTest {
        val picker = TestDashboardRunnersComponent(DashboardRunnersState(visible = true))
        picker.creation.state.value = DashboardRunnerCreationState(connections = listOf(owner))
        setGoldenContent(this) { DashboardContent(TestDashboardComponent(runners = picker)) }
        onNodeWithTag("dashboard.runners.add").performClick()
        captureGolden(this, "dashboard.runner.connections")
        onNodeWithTag("dashboard.runner.connection.1").performClick()
        assertEquals(1L, picker.creation.selected)
        assertEquals(false, picker.state.value.visible)
    }

    @Test
    fun newRunnerWithoutConnectionsExplainsSetup() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = "dashboard.runner.connections") {
            DashboardRunnerCreationDialog(
                TestDashboardRunnerCreationComponent(
                    DashboardRunnerCreationState(visible = true),
                ),
            )
        }
    }
}
