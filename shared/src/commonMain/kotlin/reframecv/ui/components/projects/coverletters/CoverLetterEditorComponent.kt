package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.ui.context.AppComponentContext

/** Distinguishes save feedback from failures in the letter editor. */
enum class LetterNotice { None, Saved, SaveFailed, DeleteFailed, Validation }

/** Keeps letter edits local until Save and reserves feedback without moving actions. */
data class CoverLetterEditorState(
    val record: CoverLetter,
    val busy: Boolean = false,
    val confirming: Boolean = false,
    val notice: LetterNotice = LetterNotice.None,
)

/** Lets a vacancy open, revise, and permanently delete a project letter. */
interface CoverLetterEditorComponent {
    val state: Value<CoverLetterEditorState>
    fun onChange(record: CoverLetter)
    fun onSave()
    fun onDelete()
    fun onCancelDelete()
    fun onClose()
}

/** Saves letter drafts without leaving the page and confirms destructive deletion. */
class DefaultCoverLetterEditorComponent(
    private val context: AppComponentContext,
    record: CoverLetter,
    private val close: () -> Unit,
    private val deleted: (Long) -> Unit,
) : CoverLetterEditorComponent {
    override val state = MutableValue(CoverLetterEditorState(record))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var notification: Job? = null
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
    }
    override fun onChange(record: CoverLetter) {
        val old = state.value
        if (!old.busy &&
            !old.confirming
        ) {
            state.value =
                old.copy(
                    record = old.record.copy(
                        name = record.name,
                        content = record.content,
                        keywords = record.keywords,
                    ),
                )
        }
    }
    override fun onSave() {
        if (state.value.busy || state.value.confirming) return
        val record = state.value.record
        if (record.name.isBlank()) {
            show(LetterNotice.Validation)
            return
        }
        perform("Save cover letter", LetterNotice.SaveFailed) {
            context.dependencies.coverLettersUseCases.save(record)
            show(LetterNotice.Saved)
        }
    }
    override fun onDelete() {
        if (state.value.busy) return
        if (!state.value.confirming) {
            state.value = state.value.copy(confirming = true)
            return
        }
        val id = state.value.record.id
        perform("Delete cover letter", LetterNotice.DeleteFailed) {
            context.dependencies.coverLettersUseCases.delete(id)
            deleted(id)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun perform(operation: String, failure: LetterNotice, action: suspend () -> Unit) {
        state.value = state.value.copy(busy = true, notice = LetterNotice.None)
        scope.launch {
            try {
                action()
            } catch (
                error: CancellationException,
            ) {
                throw error
            } catch (error: Exception) {
                context.dependencies.logger.error(
                    operation,
                    error,
                    mapOf("letterId" to state.value.record.id),
                )
                show(failure)
            } finally {
                state.value = state.value.copy(busy = false)
            }
        }
    }
    private fun show(notice: LetterNotice) {
        notification?.cancel()
        state.value = state.value.copy(notice = notice)
        notification =
            scope.launch {
                delay(LETTER_NOTICE_MILLIS)
                state.value =
                    state.value.copy(notice = LetterNotice.None)
            }
    }
    override fun onCancelDelete() {
        if (!state.value.busy) {
            state.value =
                state.value.copy(confirming = false)
        }
    }
    override fun onClose() {
        if (!state.value.busy) {
            if (state.value.confirming) onCancelDelete() else close()
        }
    }
}
private const val LETTER_NOTICE_MILLIS = 3000L
