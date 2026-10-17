package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue

class TestVacancyImportComponent(private val imported: () -> Unit = {}) : VacancyImportComponent {
    override val state = MutableValue(VacancyImportState.Idle)
    override fun onImport() = imported()
    override fun onReset() {
        state.value = VacancyImportState.Idle
    }
    override fun onClose() = Unit
}
