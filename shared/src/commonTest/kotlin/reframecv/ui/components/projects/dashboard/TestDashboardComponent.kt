package reframecv.ui.components.projects.dashboard

import com.arkivanov.decompose.value.MutableValue

class TestDashboardComponent(
    override val projectId: Long = 1,
    initialState: DashboardState = DashboardState.Ready(),
    private val openVacancies: () -> Unit = {},
    private val openResumes: () -> Unit = {},
    private val retry: () -> Unit = {},
    private val back: () -> Unit = {},
) : DashboardComponent {
    override val uiState = MutableValue(initialState)
    override fun onOpenResumes() = openResumes()
    override fun onOpenVacancies() = openVacancies()
    override fun onBack() = back()
    override fun onRetry() = retry()
}
