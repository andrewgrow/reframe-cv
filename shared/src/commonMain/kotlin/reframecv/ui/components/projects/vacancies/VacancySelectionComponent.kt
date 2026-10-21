package reframecv.ui.components.projects.vacancies

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
data class VacancySelectionState(
    val selected: Set<Long> = emptySet(),
    val confirming: Boolean = false,
    val deleting: Boolean = false,
    val failed: Boolean = false,
)

/** Lets the list select vacancies independently of opening their editors. */
interface VacancySelectionComponent {
    val state: Value<VacancySelectionState>
    fun onToggle(id: Long)
    fun onToggleAll()
    fun onRequestDelete()
    fun onConfirmDelete()
    fun onCancelDelete()
}

/** Keeps selection valid as records change and deletes the confirmed set atomically. */
class DefaultVacancySelectionComponent(
    private val context: AppComponentContext,
    private val projectId: Long,
    private val records: Value<VacanciesState>,
    private val canEdit: () -> Boolean,
) : VacancySelectionComponent {
    override val state = MutableValue(VacancySelectionState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    init {
        val subscription = records.subscribe { ready ->
            if (ready is VacanciesState.Ready) {
                val selected = if (state.value.deleting) {
                    state.value.selected
                } else {
                    state.value.selected.intersect(
                        ready.vacancies.map {
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
        val ready = records.value as? VacanciesState.Ready ?: return
        if (ready.vacancies.any { it.id == id && it.projectId == projectId }) {
            val ids = state.value.selected
            state.value =
                state.value.copy(selected = if (id in ids) ids - id else ids + id, failed = false)
        }
    }
    override fun onToggleAll() {
        if (!editable()) return
        val ids =
            (records.value as? VacanciesState.Ready)?.vacancies?.map { it.id }?.toSet() ?: return
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
                context.dependencies.vacanciesUseCases.deleteMany(projectId, ids)
                state.value = VacancySelectionState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                context.dependencies.logger.error(
                    "Delete selected vacancies",
                    error,
                    mapOf("projectId" to projectId, "vacancyIds" to ids.toList()),
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
