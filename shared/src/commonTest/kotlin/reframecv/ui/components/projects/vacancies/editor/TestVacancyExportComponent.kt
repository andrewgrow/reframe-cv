package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue

class TestVacancyExportComponent(private val download: () -> Unit = {}) : VacancyExportComponent {
    override val state = MutableValue(VacancyExportState.Idle)
    override fun onDownload() = download()
    override fun onReset() {
        if (state.value != VacancyExportState.Exporting) state.value = VacancyExportState.Idle
    }
}
