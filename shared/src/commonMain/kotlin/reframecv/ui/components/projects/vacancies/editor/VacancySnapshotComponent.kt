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
import reframecv.domain.models.vacancyattachment.VacancyAttachment
import reframecv.shared.nowMillis
import reframecv.ui.context.AppComponentContext

/** Supplies snapshot metadata, a pending selection, and file availability to the editor. */
data class VacancySnapshotState(
    val record: VacancyAttachment? = null,
    val selected: SelectedAttachment? = null,
    val status: AttachmentStatus? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val selecting: Boolean = false,
    val parsedAt: Long? = record?.parsedAt,
)

/** Lets the vacancy editor select, inspect, or detach an original snapshot. */
interface VacancySnapshotComponent {
    val state: Value<VacancySnapshotState>
    val change: SnapshotChange
    fun onAttach()
    fun onRemove()
    fun onOpen()
    fun onRetry()
    fun onParsed()
    fun onSaved(vacancyId: Long? = null)
}

/** Holds pending attachment changes until the vacancy itself is saved. */
class DefaultVacancySnapshotComponent(
    componentContext: AppComponentContext,
    private var vacancyId: Long?,
    private var hasSnapshot: Boolean,
    private val canEdit: () -> Boolean = { true },
    private val changed: () -> Unit = {},
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

    override fun onAttach() {
        if (!canEdit()) return
        runOperation("Select snapshot file", selecting = true) {
            val selection = dependencies.vacancyFilesUseCases.chooseAttachment()
            if (selection != null) {
                change = SnapshotChange.Attach(selection)
                state.value = VacancySnapshotState(selected = selection)
                changed()
            }
        }
    }

    override fun onRemove() {
        if (!state.value.loading && canEdit()) {
            change = if (hasSnapshot) SnapshotChange.Remove else SnapshotChange.Keep
            state.value = VacancySnapshotState()
            changed()
            dependencies.eventHistory.record("VacancySnapshot Remove(vacancyId=$vacancyId)")
        }
    }

    override fun onOpen() = runOperation("Open snapshot file") {
        val id = checkNotNull(vacancyId)
        load(id)
        if (state.value.status == AttachmentStatus.Available) {
            dependencies.vacanciesUseCases.openSnapshot(id)
        }
    }

    override fun onRetry() = runOperation("Load snapshot") {
        if (change == SnapshotChange.Keep || change is SnapshotChange.Parsed) {
            vacancyId?.let { load(it) }
        }
    }

    override fun onParsed() {
        val timestamp = nowMillis()
        val current = change
        change = when (current) {
            is SnapshotChange.Attach -> current.copy(parsedAt = timestamp)

            is SnapshotChange.Parsed -> current.copy(parsedAt = timestamp)

            SnapshotChange.Keep -> state.value.record?.let {
                SnapshotChange.Parsed(it.id, timestamp)
            }
                ?: return

            SnapshotChange.Remove -> return
        }
        state.value = state.value.copy(parsedAt = timestamp)
    }

    override fun onSaved(vacancyId: Long?) {
        if (vacancyId != null) this.vacancyId = vacancyId
        if (change == SnapshotChange.Keep) return
        hasSnapshot = when (change) {
            is SnapshotChange.Attach, is SnapshotChange.Parsed -> true
            SnapshotChange.Remove -> false
            SnapshotChange.Keep -> hasSnapshot
        }
        change = SnapshotChange.Keep
        state.value = VacancySnapshotState(parsedAt = state.value.parsedAt)
        if (hasSnapshot) onRetry()
    }

    private suspend fun load(id: Long) {
        val snapshot = dependencies.vacanciesUseCases.loadSnapshot(id)
        state.value = VacancySnapshotState(
            record = snapshot?.record,
            status = snapshot?.status,
            loading = true,
            parsedAt = (change as? SnapshotChange.Parsed)?.takeIf {
                it.attachmentId == snapshot?.record?.id
            }?.parsedAt ?: snapshot?.record?.parsedAt,
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
