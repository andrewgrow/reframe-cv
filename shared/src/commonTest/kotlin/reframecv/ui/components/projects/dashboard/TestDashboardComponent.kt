package reframecv.ui.components.projects.dashboard

import com.arkivanov.decompose.value.MutableValue

class TestDashboardComponent(
    override val projectId: Long = 1,
    initialState: DashboardState = DashboardState.Ready(),
    private val retry: () -> Unit = {},
) : DashboardComponent {
    override val uiState = MutableValue(initialState)
    override fun onRetry() = retry()
}
