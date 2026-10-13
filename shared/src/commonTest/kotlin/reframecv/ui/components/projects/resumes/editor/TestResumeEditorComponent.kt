package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue

class TestResumeEditorComponent(
    initialDraft: ResumeDraft = ResumeDraft(),
    private val save: (ResumeDraft) -> Unit = {},
    private val close: () -> Unit = {},
) : ResumeEditorComponent {
    override val draft = MutableValue(initialDraft)
    override val saveState = MutableValue(ResumeSaveState.Idle)
    override val validationRequested = MutableValue(false)
    override fun onDraftChange(draft: ResumeDraft) {
        if (!isBusy) this.draft.value = draft
    }
    override fun onSave() {
        if (isBusy) return
        validationRequested.value = true
        if (draft.value.isValid) save(draft.value)
    }
    override fun onClose() {
        if (!isBusy) close()
    }
}
