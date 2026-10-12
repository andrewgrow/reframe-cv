package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.attachments.AttachmentStatus
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.domain.models.importrecord.ImportRecord
import reframecv.ui.context.AppComponentContext

/** Supplies snapshot metadata, a pending selection, and file availability to the editor. */
data class VacancySnapshotState(
    val record: ImportRecord? = null,
    val selected: SelectedAttachment? = null,
    val status: AttachmentStatus? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val selecting: Boolean = false,
)

/** Lets the vacancy editor select, inspect, or detach an original snapshot. */
interface VacancySnapshotComponent {
    val state: Value<VacancySnapshotState>
    val change: SnapshotChange
    fun onAttach()
    fun onRemove()
    fun onOpen()
    fun onRetry()
}

/** Holds pending attachment changes until the vacancy itself is saved. */
class DefaultVacancySnapshotComponent(
    componentContext: AppComponentContext,
    private val vacancyId: Long?,
    private val hasSnapshot: Boolean,
) : VacancySnapshotComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(VacancySnapshotState())
    override var change: SnapshotChange = SnapshotChange.Keep
        private set

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
        if (hasSnapshot) onRetry()
    }

    override fun onAttach() = runOperation("Select snapshot file", selecting = true) {
        val selection = dependencies.attachmentPicker.pick()
        if (selection != null) {
            change = SnapshotChange.Attach(selection)
            state.value = VacancySnapshotState(selected = selection)
        }
    }

    override fun onRemove() {
        if (!state.value.loading) {
            change = if (hasSnapshot) SnapshotChange.Remove else SnapshotChange.Keep
            state.value = VacancySnapshotState()
            dependencies.eventHistory.record("VacancySnapshot Remove(vacancyId=$vacancyId)")
        }
    }

    override fun onOpen() = runOperation("Open snapshot file") {
        val id = checkNotNull(vacancyId)
        load(id)
        if (state.value.status == AttachmentStatus.Available) {
            dependencies.vacanciesRepository.openSnapshot(id)
        }
    }

    override fun onRetry() = runOperation("Load snapshot") {
        if (change == SnapshotChange.Keep) vacancyId?.let { load(it) }
    }

    private suspend fun load(id: Long) {
        val snapshot = dependencies.vacanciesRepository.snapshot(id)
        state.value = VacancySnapshotState(
            record = snapshot?.record,
            status = snapshot?.status,
            loading = true,
        )
    }

    // Picker, file, and repository implementations report different operation failures.
    @Suppress("TooGenericExceptionCaught")
    private fun runOperation(
        operation: String,
        selecting: Boolean = false,
        action: suspend () -> Unit,
    ) {
        if (state.value.loading) return
        dependencies.eventHistory.record("VacancySnapshot $operation(vacancyId=$vacancyId)")
        state.value = state.value.copy(loading = true, failed = false, selecting = selecting)
        scope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(operation, error, mapOf("vacancyId" to vacancyId))
                state.value = state.value.copy(failed = true)
            } finally {
                state.value = state.value.copy(loading = false, selecting = false)
            }
        }
    }
}
