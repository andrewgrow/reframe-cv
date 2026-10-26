package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue

class TestVacancyExportComponent(private val download: () -> Unit = {}) : VacancyExportComponent {
    override val state = MutableValue(VacancyExportState.Idle)
    override val format = MutableValue(VacancyExportFormat.Pdf)
    override fun onFormatChange(format: VacancyExportFormat) {
        if (state.value != VacancyExportState.Exporting) this.format.value = format
    }
    override fun onDownload() = download()
    override fun onReset() {
        if (state.value != VacancyExportState.Exporting) {
            state.value = VacancyExportState.Idle
            format.value = VacancyExportFormat.Pdf
        }
    }
}
