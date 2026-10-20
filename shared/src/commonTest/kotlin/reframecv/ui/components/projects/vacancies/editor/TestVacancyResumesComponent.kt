package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.vacancy.VacancyResume

class TestVacancyResumesComponent(
    private val current: () -> List<VacancyResume>,
    private val changed: (List<VacancyResume>) -> Unit,
) : VacancyResumesComponent {
    override val state = MutableValue(VacancyResumesState())
    var openedId: Long? = null
    override fun onOpen(id: Long) {
        openedId = id
    }
    override fun onSelect(id: Long) {
        if (current().none { it.resumeId == id }) changed(current() + VacancyResume(id))
    }
    override fun onUnlink(id: Long) {
        state.value = state.value.copy(unlinkId = id)
    }
    override fun onConfirmUnlink() {
        changed(current().filterNot { it.resumeId == state.value.unlinkId })
        onCancelUnlink()
    }
    override fun onCancelUnlink() {
        state.value = state.value.copy(unlinkId = null)
    }
    override fun onRetry() = Unit
}
