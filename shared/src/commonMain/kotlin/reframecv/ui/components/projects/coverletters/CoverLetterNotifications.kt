package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LETTER_NOTICE_MILLIS = 3000L

/** Replaces transient editor feedback and clears it without moving the page layout. */
internal class CoverLetterNotifications(
    private val scope: CoroutineScope,
    private val state: MutableValue<CoverLetterEditorState>,
) {
    private var pending: Job? = null

    fun show(notice: LetterNotice) {
        pending?.cancel()
        state.value = state.value.copy(notice = notice)
        pending = scope.launch {
            delay(LETTER_NOTICE_MILLIS)
            state.value = state.value.copy(notice = LetterNotice.None)
        }
    }
}
