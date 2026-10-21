package reframecv.ui.components.settings

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.export.DocumentExportResult
import reframecv.settings.SettingsImportReport
import reframecv.ui.context.AppComponentContext

/** Identifies a file operation and its retryable failure. */
enum class SettingsTransferOperation { Import, Export }

/** Keeps credential consent separate from file progress and import feedback. */
data class SettingsTransferState(
    val includeApiKeys: Boolean = false,
    val operation: SettingsTransferOperation? = null,
    val error: SettingsTransferOperation? = null,
    val report: SettingsImportReport? = null,
    val exported: Boolean = false,
)

/** Lets settings manage portable backups independently of provider editors. */
interface SettingsTransferComponent {
    val state: Value<SettingsTransferState>
    fun onIncludeApiKeys(value: Boolean)
    fun onImport()
    fun onExport()
    fun onBack()
}

/** Coordinates backup operations while preventing overlapping writes or navigation. */
class DefaultSettingsTransferComponent(context: AppComponentContext, private val back: () -> Unit) :
    SettingsTransferComponent {
    private val useCases = context.dependencies.settingsBackupUseCases
    private val logger = context.dependencies.logger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(SettingsTransferState())
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
    }
    override fun onIncludeApiKeys(value: Boolean) {
        if (state.value.operation == null) state.value = state.value.copy(includeApiKeys = value)
    }
    override fun onImport() = run(SettingsTransferOperation.Import) {
        val report = useCases.importSettings()
        state.value = state.value.copy(report = report)
    }
    override fun onExport() {
        val includeKeys = state.value.includeApiKeys
        run(SettingsTransferOperation.Export) {
            val result = useCases.export(includeKeys)
            state.value = state.value.copy(exported = result == DocumentExportResult.Saved)
        }
    }
    override fun onBack() {
        if (state.value.operation == null) {
            state.value = SettingsTransferState()
            back()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun run(operation: SettingsTransferOperation, action: suspend () -> Unit) {
        if (state.value.operation != null) return
        state.value =
            state.value.copy(operation = operation, error = null, report = null, exported = false)
        scope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error("${operation.name} settings backup", error, emptyMap())
                state.value = state.value.copy(error = operation)
            } finally {
                state.value = state.value.copy(operation = null)
            }
        }
    }
}
