package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.ui.context.AppComponentContext

/** Identifies runner validation and storage feedback without exposing search inputs in logs. */
enum class RunnerEditorError { Validation, Save, Delete, Configuration }

/** Keeps search settings in a draft until Save and tracks deletion confirmation separately. */
data class RunnerEditorState(
    val runner: ConnectionRunner,
    val draft: RunnerDraft = RunnerDraft.from(runner),
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val confirming: Boolean = false,
    val error: RunnerEditorError? = null,
    val successfulSaves: Long = 0,
) {
    val busy: Boolean get() = saving || deleting
}

/** Edits one reusable operation without starting a remote request. */
interface RunnerEditorComponent {
    val state: Value<RunnerEditorState>
    fun onDraft(draft: RunnerDraft)
    fun onSave()
    fun onDelete()
    fun onConfirmDelete()
    fun onBack()
}

/** Saves settings in place and deletes runners only after explicit confirmation. */
class DefaultRunnerEditorComponent(
    context: AppComponentContext,
    runner: ConnectionRunner,
    private val back: () -> Unit,
) : RunnerEditorComponent {
    private val useCases = context.dependencies.connectionRunnersUseCases
    private val logger = context.dependencies.logger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(
        RunnerEditorState(
            runner,
            error =
                if (runner.id != 0L && RunnerConfigurationJson.decode(runner) == null) {
                    RunnerEditorError.Configuration
                } else {
                    null
                },
        ),
    )
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
    }
    override fun onDraft(draft: RunnerDraft) {
        if (!state.value.busy && !state.value.confirming) {
            state.value = state.value.copy(draft = draft, error = null)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onSave() {
        val draft = state.value
        if (draft.busy || draft.confirming) return
        if (!draft.draft.valid) {
            state.value = draft.copy(error = RunnerEditorError.Validation)
            return
        }
        state.value = draft.copy(saving = true, error = null)
        scope.launch {
            try {
                val record = draft.draft.applyTo(draft.runner)
                val id = useCases.save(record)
                val saved = record.copy(id = id)
                state.value = draft.copy(
                    runner = saved,
                    draft = RunnerDraft.from(saved),
                    successfulSaves = draft.successfulSaves + 1,
                    error = null,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error(
                    "Save connection runner",
                    error,
                    mapOf(
                        "connectionId" to draft.runner.connectionId,
                        "runnerId" to draft.runner.id,
                    ),
                )
                state.value = draft.copy(error = RunnerEditorError.Save)
            }
        }
    }
    override fun onDelete() {
        if (!state.value.busy && state.value.runner.id != 0L) {
            state.value = state.value.copy(confirming = true, error = null)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onConfirmDelete() {
        val draft = state.value
        if (draft.busy || !draft.confirming) return
        state.value = draft.copy(deleting = true, error = null)
        scope.launch {
            try {
                useCases.delete(draft.runner.id)
                state.value = draft.copy(confirming = false)
                back()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error(
                    "Delete connection runner",
                    error,
                    mapOf(
                        "connectionId" to draft.runner.connectionId,
                        "runnerId" to draft.runner.id,
                    ),
                )
                state.value = draft.copy(error = RunnerEditorError.Delete)
            }
        }
    }
    override fun onBack() {
        if (state.value.busy) return
        if (state.value.confirming) {
            state.value = state.value.copy(confirming = false, error = null)
        } else {
            back()
        }
    }
}
