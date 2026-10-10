package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.value.MutableValue

class TestVacanciesComponent(
    override val projectId: Long = 1,
    initialState: VacanciesState = VacanciesState.Ready(emptyList()),
    private val retry: () -> Unit = {},
    private val back: () -> Unit = {},
) : VacanciesComponent {
    override val uiState = MutableValue(initialState)
    override fun onRetry() = retry()
    override fun onBack() = back()
}
