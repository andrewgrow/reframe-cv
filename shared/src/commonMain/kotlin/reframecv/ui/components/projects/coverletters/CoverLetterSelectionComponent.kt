package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.ui.context.AppComponentContext

/** Retains selected identities and a retryable bulk-deletion confirmation. */
data class CoverLetterSelectionState(
    val selected: Set<Long> = emptySet(),
    val confirming: Boolean = false,
    val deleting: Boolean = false,
    val failed: Boolean = false,
)

/** Lets the list select cover letters independently of opening their editors. */
interface CoverLetterSelectionComponent {
    val state: Value<CoverLetterSelectionState>
    fun onToggle(id: Long)
    fun onToggleAll()
    fun onRequestDelete()
    fun onConfirmDelete()
    fun onCancelDelete()
}

/** Keeps selection valid as records change and deletes the confirmed set atomically. */
class DefaultCoverLetterSelectionComponent(
    private val context: AppComponentContext,
    private val projectId: Long,
    private val records: Value<CoverLettersState>,
    private val canEdit: () -> Boolean,
) : CoverLetterSelectionComponent {
    override val state = MutableValue(CoverLetterSelectionState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    init {
        val subscription = records.subscribe { ready ->
            if (ready is CoverLettersState.Ready) {
                val selected = if (state.value.deleting) {
                    state.value.selected
                } else {
                    state.value.selected.intersect(
                        ready.letters.map {
                            it.id
                        }.toSet(),
                    )
                }
                state.value =
                    state.value.copy(
                        selected = selected,
                        confirming =
                            state.value.confirming &&
                                (selected.isNotEmpty() || state.value.deleting),
                    )
            }
        }
        context.lifecycle.doOnDestroy {
            subscription.cancel()
            scope.cancel()
        }
    }
    override fun onToggle(id: Long) {
        if (!editable()) return
        val ready = records.value as? CoverLettersState.Ready ?: return
        if (ready.letters.any { it.id == id && it.projectId == projectId }) {
            val ids = state.value.selected
            state.value =
                state.value.copy(selected = if (id in ids) ids - id else ids + id, failed = false)
        }
    }
    override fun onToggleAll() {
        if (!editable()) return
        val ids =
            (records.value as? CoverLettersState.Ready)?.letters?.map { it.id }?.toSet()
                ?: return
        state.value =
            state.value.copy(
                selected = if (state.value.selected ==
                    ids
                ) {
                    emptySet()
                } else {
                    ids
                },
                failed = false,
            )
    }
    override fun onRequestDelete() {
        if (editable() &&
            state.value.selected.isNotEmpty()
        ) {
            state.value =
                state.value.copy(confirming = true, failed = false)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onConfirmDelete() {
        if (!canEdit() || state.value.deleting || !state.value.confirming) return
        val ids = state.value.selected
        if (ids.isEmpty()) {
            onCancelDelete()
            return
        }
        state.value = state.value.copy(deleting = true, failed = false)
        scope.launch {
            try {
                context.dependencies.coverLettersUseCases.deleteMany(projectId, ids)
                state.value = CoverLetterSelectionState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                context.dependencies.logger.error(
                    "Delete selected cover letters",
                    error,
                    mapOf("projectId" to projectId, "letterIds" to ids.toList()),
                )
                state.value = state.value.copy(deleting = false, failed = true)
            }
        }
    }
    override fun onCancelDelete() {
        if (!state.value.deleting) {
            state.value =
                state.value.copy(confirming = false, failed = false)
        }
    }
    private fun editable() = canEdit() && !state.value.confirming && !state.value.deleting
}
