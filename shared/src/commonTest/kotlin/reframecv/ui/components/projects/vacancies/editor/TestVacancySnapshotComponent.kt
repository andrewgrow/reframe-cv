package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.attachments.SnapshotChange

class TestVacancySnapshotComponent : VacancySnapshotComponent {
    override val state = MutableValue(VacancySnapshotState())
    override var change: SnapshotChange = SnapshotChange.Keep
    override fun onAttach() = Unit
    override fun onRemove() {
        state.value = VacancySnapshotState()
    }
    override fun onOpen() = Unit
    override fun onSaved() {
        change = SnapshotChange.Keep
        state.value = state.value.copy(selected = null)
    }
    override fun onRetry() = Unit
}
