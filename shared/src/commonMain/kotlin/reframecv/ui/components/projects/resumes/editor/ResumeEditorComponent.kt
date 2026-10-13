package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.resume.Resume
import reframecv.ui.context.AppComponentContext

/** Reports resume save progress without discarding a failed draft. */
enum class ResumeSaveState { Idle, Saving, Failed }

/** Lets creation and editing share a structured draft and save feedback. */
interface ResumeEditorComponent {
    val initialResume: Resume?
    val draft: Value<ResumeDraft>
    val saveState: Value<ResumeSaveState>
    val validationRequested: Value<Boolean>
    val isBusy: Boolean get() = saveState.value == ResumeSaveState.Saving
    fun onDraftChange(draft: ResumeDraft)
    fun onSave()
    fun onClose()
}

/** Validates resume changes and prevents dismissal while saving. */
class DefaultResumeEditorComponent(
    componentContext: AppComponentContext,
    private val save: (ResumeDraft) -> Unit,
    private val close: () -> Unit,
    override val initialResume: Resume? = null,
    initialRole: ResumeRole = ResumeRole.Regular,
) : ResumeEditorComponent,
    AppComponentContext by componentContext {
    override val draft =
        MutableValue(
            initialResume?.let {
                ResumeDraft.fromResume(it, initialRole)
            } ?: ResumeDraft(),
        )
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
