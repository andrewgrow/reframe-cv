package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue

class TestCoverLetterSelectionComponent(private val records: () -> CoverLettersState) :
    CoverLetterSelectionComponent {
    override val state = MutableValue(CoverLetterSelectionState())
    override fun onToggle(id: Long) {
        val ids = state.value.selected
        state.value =
            state.value.copy(selected = if (id in ids) ids - id else ids + id)
    }
    override fun onToggleAll() {
        val ids = (records() as? CoverLettersState.Ready)?.letters.orEmpty().map { it.id }.toSet()
        state.value =
            state.value.copy(selected = if (state.value.selected == ids) emptySet() else ids)
    }
    override fun onRequestDelete() {
        state.value = state.value.copy(confirming = true)
    }
    override fun onConfirmDelete() {
        state.value = CoverLetterSelectionState()
    }
    override fun onCancelDelete() {
        state.value = state.value.copy(confirming = false)
    }
}
