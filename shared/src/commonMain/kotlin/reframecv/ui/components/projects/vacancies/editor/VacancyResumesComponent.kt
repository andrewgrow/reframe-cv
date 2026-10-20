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
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.ui.context.AppComponentContext

/** Supplies available project resumes and pending unlink confirmation to the vacancy page. */
data class VacancyResumesState(
    val available: List<Resume> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val unlinkId: Long? = null,
)

/** Lets the vacancy page attach resumes and confirm unlinking without deleting resumes. */
interface VacancyResumesComponent {
    val state: Value<VacancyResumesState>
    fun onOpen(id: Long)
    fun onSelect(id: Long)
    fun onUnlink(id: Long)
    fun onConfirmUnlink()
    fun onCancelUnlink()
    fun onRetry()
}

/** Keeps resume links in the vacancy draft until Save and observes available project resumes. */
class DefaultVacancyResumesComponent(
    componentContext: AppComponentContext,
    private val projectId: Long,
    private val current: () -> List<VacancyResume>,
    private val changed: (List<VacancyResume>) -> Unit,
    private val canEdit: () -> Boolean,
    private val open: (Resume) -> Unit = {},
) : VacancyResumesComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    override val state = MutableValue(VacancyResumesState())

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
        onRetry()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRetry() {
        observation?.cancel()
        state.value = state.value.copy(loading = true, failed = false)
        observation = scope.launch {
            try {
                dependencies.resumesUseCases.observe(projectId).collect { records ->
                    state.value = state.value.copy(available = records.resumes, loading = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Load vacancy resume choices",
                    error,
                    mapOf("projectId" to projectId),
                )
                state.value = state.value.copy(loading = false, failed = true)
            }
        }
    }

    fun onCreated(record: Resume) {
        require(record.projectId == projectId)
        state.value =
            state.value.copy(
                available = (
                    state.value.available.filterNot { it.id == record.id } +
                        record
                    ).sortedBy { it.id },
            )
    }

    override fun onOpen(id: Long) {
        if (!canEdit() || state.value.unlinkId != null ||
            current().none { it.resumeId == id }
        ) {
            return
        }
        state.value.available.find { it.id == id && it.projectId == projectId }?.let(open)
    }

    override fun onSelect(id: Long) {
        if (!canEdit() || state.value.unlinkId != null) return
        val available = state.value.available.any { it.id == id && it.projectId == projectId }
        if (!available || current().any { it.resumeId == id }) return
        changed(current() + VacancyResume(id))
    }

    override fun onUnlink(id: Long) {
        if (canEdit() && current().any { it.resumeId == id }) {
            state.value = state.value.copy(unlinkId = id)
        }
    }

    override fun onConfirmUnlink() {
        if (!canEdit()) return
        val id = state.value.unlinkId ?: return
        changed(current().filterNot { it.resumeId == id })
        onCancelUnlink()
    }

    override fun onCancelUnlink() {
        state.value = state.value.copy(unlinkId = null)
    }
}
