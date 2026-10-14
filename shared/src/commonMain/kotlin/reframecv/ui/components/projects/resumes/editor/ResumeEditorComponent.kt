package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.resume.Resume
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_delete_confirm
import reframecv.shared.generated.resources.resume_delete_error
import reframecv.shared.generated.resources.resume_delete_prompt
import reframecv.shared.generated.resources.resume_delete_title
import reframecv.shared.generated.resources.resume_delete_warning
import reframecv.ui.components.common.confirmation.ConfirmationDialogAction
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.context.AppComponentContext

/** Reports resume save progress without discarding a failed draft. */
enum class ResumeSaveState { Idle, Saving, Saved, Failed }

/** Keeps typed deletion confirmation and failed deletions on the management page. */
enum class ResumeDeleteState { Idle, Confirming, Deleting, Failed }

/** Lets creation and editing share a structured draft and save feedback. */
interface ResumeEditorComponent {
    val initialResume: Resume?
    val draft: Value<ResumeDraft>
    val saveState: Value<ResumeSaveState>
    val successfulSaves: Value<Long>
    val validationFailures: Value<Long>
    val deleteState: Value<ResumeDeleteState>
    val deleteConfirmation: Value<String>
    val exportVisible: Value<Boolean>
    val exportComponent: ResumeExportComponent
    val importComponent: ResumeImportComponent
    val photoComponent: ResumePhotoComponent
    val validationRequested: Value<Boolean>
    val isBusy: Boolean get() = saveState.value == ResumeSaveState.Saving ||
        deleteState.value == ResumeDeleteState.Deleting ||
        exportComponent.state.value == ResumeExportState.Exporting ||
        importComponent.state.value == ResumeImportState.Importing ||
        photoComponent.state.value.loading
    val canConfirmDelete: Boolean get() = initialResume != null && !isBusy &&
        deleteState.value != ResumeDeleteState.Idle &&
        deleteConfirmation.value.isResumeDeleteConfirmation()
    val deleteDialogContent: ConfirmationDialogContent get() = ConfirmationDialogContent(
        title = Res.string.resume_delete_title,
        message = Res.string.resume_delete_warning,
        confirmLabel = Res.string.resume_delete_confirm,
        cancelLabel = Res.string.resume_cancel,
        subject = initialResume?.name,
        inputLabel = Res.string.resume_delete_prompt,
        failureMessage = Res.string.resume_delete_error,
        tone = ConfirmationDialogTone.Critical,
    )
    fun onDeleteDialogAction(action: ConfirmationDialogAction) {
        when (action) {
            ConfirmationDialogAction.Back, ConfirmationDialogAction.Cancel -> onCancelDelete()
            ConfirmationDialogAction.Confirm -> onConfirmDelete()
        }
    }
    fun onRequestDelete()
    fun onDeleteConfirmationChange(value: String)
    fun onConfirmDelete()
    fun onCancelDelete()
    fun onDraftChange(draft: ResumeDraft)
    fun onSave()
    fun onExport()
    fun onImport() {
        if (initialResume == null && !isBusy && deleteState.value == ResumeDeleteState.Idle) {
            importComponent.onImport()
        }
    }

    fun onCloseExport()
    fun onClose()
}

/** Validates resume changes and prevents dismissal while saving. */
class DefaultResumeEditorComponent(
    componentContext: AppComponentContext,
    private val save: (ResumeDraft) -> Unit,
    private val close: () -> Unit,
    override val initialResume: Resume? = null,
    initialRole: ResumeRole = ResumeRole.Regular,
    private val delete: () -> Unit = {},
) : ResumeEditorComponent,
    AppComponentContext by componentContext {
    override val draft =
        MutableValue(
            initialResume?.let {
                ResumeDraft.fromResume(it, initialRole)
            } ?: ResumeDraft(),
        )
    override val deleteState = MutableValue(ResumeDeleteState.Idle)
    override val deleteConfirmation = MutableValue("")
    override val saveState = MutableValue(ResumeSaveState.Idle)
    override val successfulSaves = MutableValue(0L)
    override val validationFailures = MutableValue(0L)
    override val validationRequested = MutableValue(false)
    override val exportVisible = MutableValue(false)
    override val exportComponent = DefaultResumeExportComponent(
        componentContext,
        resume = {
            initialResume?.takeIf { exportVisible.value }?.let { draft.value.applyTo(it) }
        },
        finished = ::onCloseExport,
    )
    override val importComponent = DefaultResumeImportComponent(componentContext) { content ->
        draft.value = draft.value.withImportedContent(content)
        validationRequested.value = false
        saveState.value = ResumeSaveState.Idle
    }
    override val photoComponent =
        DefaultResumePhotoComponent(componentContext, initialResume?.photoPath) {
            draft.value = draft.value.copy(photoPath = it)
        }
    internal var savedRole = initialRole
        private set
    private var exportAfterSave = false

    override fun onDraftChange(draft: ResumeDraft) {
        if (!isBusy && !exportVisible.value && deleteState.value == ResumeDeleteState.Idle) {
            importComponent.onReset()
            this.draft.value = draft
            if (saveState.value == ResumeSaveState.Saved) saveState.value = ResumeSaveState.Idle
        }
    }

    override fun onSave() = requestSave(export = false)

    override fun onExport() {
        if (initialResume != null) requestSave(export = true)
    }

    private fun requestSave(export: Boolean) {
        if (isBusy || exportVisible.value || deleteState.value != ResumeDeleteState.Idle) return
        validationRequested.value = true
        if (!draft.value.isValid) {
            if (!export) validationFailures.value += 1
            return
        }
        importComponent.onReset()
        exportAfterSave = export
        saveState.value = ResumeSaveState.Saving
        photoComponent.prepareSave { prepared ->
            if (prepared) save(draft.value) else onSaveCompleted(ResumeSaveState.Failed)
        }
    }

    internal fun onSaveCompleted(state: ResumeSaveState): Boolean {
        saveState.value = state
        if (state == ResumeSaveState.Idle) {
            photoComponent.onSaved()
            savedRole = draft.value.role
            exportVisible.value = exportAfterSave
            if (initialResume != null) {
                successfulSaves.value += 1
                saveState.value = ResumeSaveState.Saved
            }
        }
        exportAfterSave = false
        return exportVisible.value
    }

    override fun onCloseExport() {
        if (!isBusy) {
            exportVisible.value = false
            exportComponent.onReset()
        }
    }

    override fun onRequestDelete() {
        if (initialResume == null || isBusy) return
        if (!exportVisible.value && deleteState.value == ResumeDeleteState.Idle) {
            deleteConfirmation.value = ""
            deleteState.value = ResumeDeleteState.Confirming
        }
    }

    override fun onDeleteConfirmationChange(value: String) {
        if (!isBusy && deleteState.value != ResumeDeleteState.Idle) deleteConfirmation.value = value
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
            if (photoComponent.visible.value) {
                photoComponent.onClose()
            } else if (exportVisible.value) {
                onCloseExport()
            } else if (deleteState.value != ResumeDeleteState.Idle) {
                onDeleteDialogAction(ConfirmationDialogAction.Back)
            } else {
                importComponent.onClose()
                close()
            }
        }
    }
}

internal fun String.isResumeDeleteConfirmation(): Boolean =
    trim().equals("DELETE", ignoreCase = true)
