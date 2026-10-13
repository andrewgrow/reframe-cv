package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.ui.context.AppComponentContext

/** Reports resume save progress without discarding a failed draft. */
enum class ResumeSaveState { Idle, Saving, Failed }

/** Lets the creation page edit a structured draft and submit it to its project. */
interface ResumeEditorComponent {
    val draft: Value<ResumeDraft>
    val saveState: Value<ResumeSaveState>
    val validationRequested: Value<Boolean>
    val isBusy: Boolean get() = saveState.value == ResumeSaveState.Saving
    fun onDraftChange(draft: ResumeDraft)
    fun onSave()
    fun onClose()
}

/** Validates creation and blocks edits or dismissal while a resume is being saved. */
class DefaultResumeEditorComponent(
    componentContext: AppComponentContext,
    private val save: (ResumeDraft) -> Unit,
    private val close: () -> Unit,
) : ResumeEditorComponent,
    AppComponentContext by componentContext {
    override val draft = MutableValue(ResumeDraft())
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
