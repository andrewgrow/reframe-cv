package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.ui.components.common.ai.AiRequestDialogSlot

class TestVacancyLettersComponent(
    private val current: () -> List<Long> = { emptyList() },
    private val changed: (List<Long>) -> Unit = {},
) : VacancyLettersComponent {
    var openedId: Long? = null
    override val state = MutableValue(VacancyLettersState())
    override fun onSelect(id: Long) {
        if (id !in current()) changed(current() + id)
    }
    override fun onOpen(id: Long) {
        openedId = id
    }
    override fun onUnlink(id: Long) {
        state.value = state.value.copy(unlinkId = id)
    }
    override fun onConfirmUnlink() {
        state.value.unlinkId?.let { changed(current() - it) }
        onCancelUnlink()
    }
    override fun onCancelUnlink() {
        state.value = state.value.copy(unlinkId = null)
    }
    override fun onRetry() = Unit
    override fun onCreated(record: CoverLetter) = Unit
    override fun onDeleted(id: Long) = Unit
}

class TestVacancyLetterGenerationComponent : VacancyLetterGenerationComponent {
    override val selection = MutableValue(LetterResumeSelection())
    override val dialog = MutableValue(AiRequestDialogSlot())
    override fun onGenerate() = Unit
    override fun onResume(id: Long) {
        selection.value = LetterResumeSelection(true, id)
    }
    override fun onContinue() = Unit
    override fun onCancel() {
        selection.value = LetterResumeSelection()
        dialog.value =
            AiRequestDialogSlot()
    }
}
