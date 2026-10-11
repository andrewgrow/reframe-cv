package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.attachments.SnapshotChange

class TestVacancySnapshotComponent : VacancySnapshotComponent {
    override val state = MutableValue(VacancySnapshotState())
    override val change = SnapshotChange.Keep
    override fun onAttach() = Unit
    override fun onRemove() {
        state.value = VacancySnapshotState()
    }
    override fun onOpen() = Unit
    override fun onRetry() = Unit
}
