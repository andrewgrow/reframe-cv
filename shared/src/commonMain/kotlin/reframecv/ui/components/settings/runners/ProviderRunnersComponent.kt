package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
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
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.ui.context.AppComponentContext

/** Supplies a connection-scoped list without displaying an empty state before loading completes. */
data class ProviderRunnersState(
    val runners: List<ConnectionRunner> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

/** Owns the runner list and its nested settings page. */
interface ProviderRunnersComponent {
    val state: Value<ProviderRunnersState>
    val editorSlot: Value<ChildSlot<*, RunnerEditorComponent>>
    val isBusy: Boolean get() = editorSlot.value.child?.instance?.state?.value?.busy == true
    fun onAdd()
    fun onEdit(id: Long)
    fun onRetry()
    fun onBack()
}

/** Observes only the owning provider's operations and retains its draft during navigation. */
class DefaultProviderRunnersComponent(
    context: AppComponentContext,
    private val connectionId: Long,
) : ProviderRunnersComponent {
    private val useCases = context.dependencies.connectionRunnersUseCases
    private val logger = context.dependencies.logger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    private val navigation = SlotNavigation<ConnectionRunner>()
    override val state = MutableValue(ProviderRunnersState())
    override val editorSlot = context.childSlot(
        navigation,
        serializer = null,
        key = "RunnerEditor",
        handleBackButton = false,
    ) { record, childContext ->
        DefaultRunnerEditorComponent(childContext, record, back = { navigation.dismiss() })
            as RunnerEditorComponent
    }
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
        onRetry()
    }
    override fun onAdd() {
        if (!isBusy && editorSlot.value.child == null) {
            navigation.activate(RunnerDraft.newRunner(connectionId))
        }
    }
    override fun onEdit(id: Long) {
        if (isBusy || editorSlot.value.child != null) return
        state.value.runners.find { it.id == id && it.connectionId == connectionId }?.let {
            navigation.activate(it)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRetry() {
        observation?.cancel()
        state.value = state.value.copy(loading = true, failed = false)
        observation = scope.launch {
            try {
                useCases.observe(connectionId).collect { records ->
                    state.value = ProviderRunnersState(records.sortedBy { it.id }, loading = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error(
                    "Load connection runners",
                    error,
                    mapOf("connectionId" to connectionId),
                )
                state.value = state.value.copy(loading = false, failed = true)
            }
        }
    }
    override fun onBack() {
        editorSlot.value.child?.instance?.onBack()
    }
}
