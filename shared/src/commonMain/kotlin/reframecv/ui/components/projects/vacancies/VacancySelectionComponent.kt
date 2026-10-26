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
import reframecv.domain.models.vacancy.Vacancy
import reframecv.shared.nowMillis
import reframecv.ui.context.AppComponentContext

/** Retains selection and the progress of bulk vacancy actions. */
data class VacancySelectionState(
    val selected: Set<Long> = emptySet(),
    val confirming: Boolean = false,
    val deleting: Boolean = false,
    val failed: Boolean = false,
    val applying: Boolean = false,
    val applicationFailed: Boolean = false,
    val hideApplied: Boolean = false,
) {
    val busy: Boolean get() = deleting || applying
    fun visible(vacancies: List<Vacancy>): List<Vacancy> =
        if (hideApplied) vacancies.filter { it.appliedAt == null } else vacancies
}

/** Lets the list select vacancies independently of opening their editors. */
interface VacancySelectionComponent {
    val state: Value<VacancySelectionState>
    fun onToggleAppliedFilter()
    fun onMarkApplied()
    fun onToggle(id: Long)
    fun onToggleAll()
    fun onRequestDelete()
    fun onConfirmDelete()
    fun onCancelDelete()
}

/** Applies bulk actions to selected vacancies and keeps failures retryable. */
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
                val selected = if (state.value.busy) {
                    state.value.selected
                } else {
                    state.value.selected.intersect(
                        state.value.visible(ready.vacancies).map {
                            it.id
                        }.toSet(),
                    )
                }
                state.value =
                    state.value.copy(
                        selected = selected,
                        confirming =
                            state.value.confirming &&
                                (selected.isNotEmpty() || state.value.busy),
                    )
            }
        }
        context.lifecycle.doOnDestroy {
            subscription.cancel()
            scope.cancel()
        }
    }
    override fun onToggleAppliedFilter() {
        if (!editable()) return
        state.value = state.value.copy(hideApplied = !state.value.hideApplied)
        state.value = state.value.copy(selected = state.value.selected.intersect(visibleIds()))
    }
    private fun visibleIds(): Set<Long> = state.value.visible(
        (records.value as? VacanciesState.Ready)?.vacancies.orEmpty(),
    ).map { it.id }.toSet()
    override fun onToggle(id: Long) {
        if (!editable()) return
        val ready = records.value as? VacanciesState.Ready ?: return
        if (state.value.visible(ready.vacancies).any { it.id == id && it.projectId == projectId }) {
            val ids = state.value.selected
            state.value =
                state.value.copy(
                    selected = if (id in
                        ids
                    ) {
                        ids - id
                    } else {
                        ids + id
                    },
                    failed = false,
                    applicationFailed = false,
                )
        }
    }
    override fun onToggleAll() {
        if (!editable()) return
        val ids =
            visibleIds()
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
                applicationFailed = false,
            )
    }
    override fun onRequestDelete() {
        if (editable() &&
            state.value.selected.isNotEmpty()
        ) {
            state.value =
                state.value.copy(confirming = true, failed = false, applicationFailed = false)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onConfirmDelete() {
        if (!canEdit() || state.value.busy || !state.value.confirming) return
        val ids = state.value.selected
        if (ids.isEmpty()) {
            onCancelDelete()
            return
        }
        state.value = state.value.copy(deleting = true, failed = false, applicationFailed = false)
        scope.launch {
            try {
                context.dependencies.vacanciesUseCases.deleteMany(projectId, ids)
                state.value = VacancySelectionState(hideApplied = state.value.hideApplied)
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

    @Suppress("TooGenericExceptionCaught")
    override fun onMarkApplied() {
        if (!editable() || state.value.selected.isEmpty()) return
        val ids = state.value.selected.toSet()
        val timestamp = nowMillis()
        state.value = state.value.copy(applying = true, applicationFailed = false)
        scope.launch {
            try {
                context.dependencies.vacanciesUseCases.markApplied(projectId, ids, timestamp)
                state.value = state.value.copy(
                    applying = false,
                    selected = state.value.selected.intersect(visibleIds()),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                context.dependencies.logger.error(
                    "Mark selected vacancies applied",
                    error,
                    mapOf("projectId" to projectId, "vacancyIds" to ids.toList()),
                )
                state.value = state.value.copy(applying = false, applicationFailed = true)
            }
        }
    }
    override fun onCancelDelete() {
        if (!state.value.busy) {
            state.value =
                state.value.copy(confirming = false, failed = false, applicationFailed = false)
        }
    }
    private fun editable() = canEdit() && !state.value.confirming && !state.value.busy
}
