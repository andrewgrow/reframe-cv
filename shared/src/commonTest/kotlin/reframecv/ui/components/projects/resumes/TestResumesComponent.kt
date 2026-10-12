package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.value.MutableValue

class TestResumesComponent(
    override val projectId: Long = 1,
    initialState: ResumesState = ResumesState.Ready(emptyList()),
    private val retry: () -> Unit = {},
    private val back: () -> Unit = {},
) : ResumesComponent {
    override val uiState = MutableValue(initialState)
    override fun onRetry() = retry()
    override fun onBack() = back()
}
