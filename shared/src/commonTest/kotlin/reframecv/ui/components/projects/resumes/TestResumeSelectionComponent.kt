package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.value.MutableValue

class TestResumeSelectionComponent(private val records: () -> ResumesState) :
    ResumeSelectionComponent {
    override val state = MutableValue(ResumeSelectionState())
    override fun onToggle(id: Long) {
        val ids = state.value.selected
        state.value =
            state.value.copy(selected = if (id in ids) ids - id else ids + id)
    }
    override fun onToggleAll() {
        val ids = (records() as? ResumesState.Ready)?.resumes.orEmpty().map { it.id }.toSet()
        state.value =
            state.value.copy(selected = if (state.value.selected == ids) emptySet() else ids)
    }
    override fun onRequestDelete() {
        state.value = state.value.copy(confirming = true)
    }
    override fun onConfirmDelete() {
        state.value = ResumeSelectionState()
    }
    override fun onCancelDelete() {
        state.value = state.value.copy(confirming = false)
    }
}
