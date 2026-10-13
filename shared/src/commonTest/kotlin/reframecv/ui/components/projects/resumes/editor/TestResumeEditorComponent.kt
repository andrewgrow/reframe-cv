package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.resume.Resume

class TestResumeEditorComponent(
    initialDraft: ResumeDraft? = null,
    override val initialResume: Resume? = null,
    private val save: (ResumeDraft) -> Unit = {},
    private val close: () -> Unit = {},
) : ResumeEditorComponent {
    override val draft =
        MutableValue(initialDraft ?: initialResume?.let(ResumeDraft::fromResume) ?: ResumeDraft())
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
