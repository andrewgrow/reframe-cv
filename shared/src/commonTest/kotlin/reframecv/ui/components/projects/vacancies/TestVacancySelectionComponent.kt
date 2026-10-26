package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.value.MutableValue

class TestVacancySelectionComponent(private val records: () -> VacanciesState) :
    VacancySelectionComponent {
    override val state = MutableValue(VacancySelectionState())
    override fun onToggleAppliedFilter() {
        val next = state.value.copy(hideApplied = !state.value.hideApplied)
        val visible = next.visible((records() as? VacanciesState.Ready)?.vacancies.orEmpty())
        state.value = next.copy(selected = next.selected.intersect(visible.map { it.id }.toSet()))
    }
    override fun onMarkApplied() = Unit
    override fun onToggle(id: Long) {
        val ids = state.value.selected
        state.value =
            state.value.copy(selected = if (id in ids) ids - id else ids + id)
    }
    override fun onToggleAll() {
        val ids = state.value.visible((records() as? VacanciesState.Ready)?.vacancies.orEmpty())
            .map { it.id }.toSet()
        state.value =
            state.value.copy(selected = if (state.value.selected == ids) emptySet() else ids)
    }
    override fun onRequestDelete() {
        state.value = state.value.copy(confirming = true)
    }
    override fun onConfirmDelete() {
        state.value = VacancySelectionState(hideApplied = state.value.hideApplied)
    }
    override fun onCancelDelete() {
        state.value = state.value.copy(confirming = false)
    }
}
