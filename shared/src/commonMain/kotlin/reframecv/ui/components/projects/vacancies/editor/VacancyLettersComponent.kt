package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.ui.context.AppComponentContext

/** Supplies project letter choices and a pending unlink confirmation. */
data class VacancyLettersState(
    val available: List<CoverLetter> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val unlinkId: Long? = null,
)

/** Edits vacancy letter links without deleting the underlying documents. */
interface VacancyLettersComponent {
    val state: Value<VacancyLettersState>
    fun onSelect(id: Long)
    fun onOpen(id: Long)
    fun onUnlink(id: Long)
    fun onConfirmUnlink()
    fun onCancelUnlink()
    fun onRetry()
    fun onCreated(record: CoverLetter)
    fun onDeleted(id: Long)
}

/** Observes reusable letters while keeping manual link changes in the vacancy draft. */
class DefaultVacancyLettersComponent(
    private val context: AppComponentContext,
    private val projectId: Long,
    private val current: () -> List<Long>,
    private val changed: (List<Long>) -> Unit,
    private val canEdit: () -> Boolean,
    private val open: (CoverLetter) -> Unit,
) : VacancyLettersComponent {
    override val state = MutableValue(VacancyLettersState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
        onRetry()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRetry() {
        observation?.cancel()
        state.value = state.value.copy(loading = true, failed = false)
        observation = scope.launch {
            try {
                context.dependencies.coverLettersUseCases.observe(projectId).collect {
                    state.value = state.value.copy(available = it, loading = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                context.dependencies.logger.error(
                    "Load vacancy cover letters",
                    error,
                    mapOf("projectId" to projectId),
                )
                state.value = state.value.copy(loading = false, failed = true)
            }
        }
    }
    override fun onSelect(id: Long) {
        if (!canEdit() || state.value.unlinkId != null) return
        val available = state.value.available.any { it.id == id && it.projectId == projectId }
        if (available && id !in current()) changed(current() + id)
    }
    override fun onOpen(id: Long) {
        if (canEdit() && state.value.unlinkId == null &&
            id in current()
        ) {
            state.value.available.find { it.id == id }?.let(open)
        }
    }
    override fun onUnlink(id: Long) {
        if (canEdit() &&
            id in current()
        ) {
            state.value = state.value.copy(unlinkId = id)
        }
    }
    override fun onConfirmUnlink() {
        if (canEdit()) {
            state.value.unlinkId?.let { changed(current() - it) }
            onCancelUnlink()
        }
    }
    override fun onCancelUnlink() {
        state.value = state.value.copy(unlinkId = null)
    }
    override fun onCreated(record: CoverLetter) {
        require(record.projectId == projectId)
        state.value =
            state.value.copy(
                available = (
                    state.value.available.filterNot { it.id == record.id } +
                        record
                    ).sortedBy { it.id },
            )
        if (record.id !in current()) changed(current() + record.id)
    }
    override fun onDeleted(id: Long) {
        state.value = state.value.copy(available = state.value.available.filterNot { it.id == id })
        changed(current() - id)
    }
}
