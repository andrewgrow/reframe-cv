package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.resume.Resume
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction

class TestResumeEditorComponent(
    initialDraft: ResumeDraft? = null,
    override val initialResume: Resume? = null,
    initialRole: ResumeRole = ResumeRole.Regular,
    private val save: (ResumeDraft) -> Unit = {},
    private val close: () -> Unit = {},
    private val delete: () -> Unit = {},
    private val import: () -> Unit = {},
) : ResumeEditorComponent {
    override val draft =
        MutableValue(
            initialDraft ?: initialResume?.let { ResumeDraft.fromResume(it, initialRole) }
                ?: ResumeDraft(),
        )
    override val deleteState = MutableValue(ResumeDeleteState.Idle)
    override val deleteConfirmation = MutableValue("")
    override val saveState = MutableValue(ResumeSaveState.Idle)
    override val successfulSaves = MutableValue(0L)
    override val validationRequested = MutableValue(false)
    override val exportVisible = MutableValue(false)
    override val exportComponent = TestResumeExportComponent { onCloseExport() }
    override val importComponent = TestResumeImportComponent(import)
    override fun onDraftChange(draft: ResumeDraft) {
        if (!isBusy && deleteState.value == ResumeDeleteState.Idle) {
            importComponent.onReset()
            this.draft.value = draft
            if (saveState.value == ResumeSaveState.Saved) saveState.value = ResumeSaveState.Idle
        }
    }
    override fun onSave() {
        if (isBusy || deleteState.value != ResumeDeleteState.Idle) return
        validationRequested.value = true
        if (draft.value.isValid) save(draft.value)
    }
    override fun onExport() {
        if (initialResume == null || isBusy || deleteState.value != ResumeDeleteState.Idle) return
        validationRequested.value = true
        if (draft.value.isValid) {
            save(draft.value)
            exportVisible.value = true
        }
    }
    override fun onCloseExport() {
        exportVisible.value = false
    }
    override fun onRequestDelete() {
        if (initialResume != null && !isBusy) deleteState.value = ResumeDeleteState.Confirming
    }
    override fun onDeleteConfirmationChange(value: String) {
        if (!isBusy) deleteConfirmation.value = value
    }
    override fun onConfirmDelete() {
        if (canConfirmDelete) delete()
    }
    override fun onCancelDelete() {
        if (!isBusy) {
            deleteState.value = ResumeDeleteState.Idle
            deleteConfirmation.value = ""
        }
    }
    override fun onClose() {
        if (!isBusy) {
            if (exportVisible.value) {
                onCloseExport()
            } else if (deleteState.value != ResumeDeleteState.Idle) {
                onDeleteDialogAction(ConfirmationDialogAction.Back)
            } else {
                close()
            }
        }
    }
}
