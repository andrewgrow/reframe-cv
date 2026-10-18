package reframecv.ui.components.settings

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.ui.context.AppComponentContext

/** Supplies the persisted provider list and retryable operation feedback. */
data class SettingsState(
    val connections: List<ProviderConnection> = emptyList(),
    val loading: Boolean = true,
    val error: SettingsError? = null,
)

/** Lets the application open global settings and configure API connections. */
interface SettingsComponent {
    val state: Value<SettingsState>
    val editorSlot: Value<ChildSlot<*, ProviderEditorComponent>>
    fun onAdd()
    fun onEdit(connection: ProviderConnection)
    fun onActivate(id: Long)
    fun onRetry()
    fun onBack()
    val isBusy: Boolean get() = editorSlot.value.child?.instance?.state?.value?.busy == true
}

/** Observes saved connections while the connection editor owns its independent draft lifecycle. */
class DefaultSettingsComponent(
    componentContext: AppComponentContext,
    private val back: () -> Unit,
) : SettingsComponent,
    AppComponentContext by componentContext {
    private val useCases = dependencies.providerSettingsUseCases
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    override val state = MutableValue(SettingsState())
    private val editorNavigation = SlotNavigation<ProviderConnection>()
    override val editorSlot = childSlot(
        editorNavigation,
        serializer = null,
        handleBackButton = false,
    ) { connection, context ->
        DefaultProviderEditorComponent(
            context,
            connection,
            saved = { editorNavigation.dismiss() },
            back = { editorNavigation.dismiss() },
        ) as ProviderEditorComponent
    }
    init {
        val callback = BackCallback { onBack() }
        backHandler.register(callback)
        lifecycle.doOnDestroy {
            scope.cancel()
            backHandler.unregister(callback)
        }
        onRetry()
    }
    override fun onAdd() = onEdit(
        ProviderConnection(
            type = ApiProviderType.OpenRouter,
            name = "",
            apiKey = "",
            modelId = "",
        ),
    )
    override fun onEdit(connection: ProviderConnection) {
        if (!isBusy) editorNavigation.activate(connection)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onActivate(id: Long) {
        if (isBusy) return
        scope.launch {
            try {
                useCases.activate(id)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = state.value.copy(error = SettingsError.Save)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRetry() {
        observation?.cancel()
        state.value = state.value.copy(loading = true, error = null)
        observation = scope.launch {
            try {
                useCases.observe().collect {
                    state.value =
                        SettingsState(connections = it, loading = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = state.value.copy(loading = false, error = SettingsError.Load)
            }
        }
    }
    override fun onBack() {
        editorSlot.value.child?.instance?.onBack() ?: back()
    }
}
