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
import reframecv.ui.components.settings.prompts.DefaultPromptsComponent
import reframecv.ui.components.settings.prompts.PromptsComponent
import reframecv.ui.context.AppComponentContext

/** Identifies a top-level settings destination. */
enum class SettingsSection { Overview, ApiProviders }

/** Supplies the persisted provider list and retryable operation feedback. */
data class SettingsState(
    val connections: List<ProviderConnection> = emptyList(),
    val loading: Boolean = true,
    val error: SettingsError? = null,
    val changingActive: Set<Long> = emptySet(),
    val section: SettingsSection = SettingsSection.Overview,
)

/** Provides global provider and prompt settings. */
interface SettingsComponent {
    val state: Value<SettingsState>
    val editorSlot: Value<ChildSlot<*, ProviderEditorComponent>>
    val promptsSlot: Value<ChildSlot<*, PromptsComponent>>
    fun onProviders()
    fun onPrompts()
    fun onAdd()
    fun onEdit(connection: ProviderConnection)
    fun onActiveChange(id: Long, active: Boolean)
    fun onRetry()
    fun onBack()
    val isBusy: Boolean get() = editorSlot.value.child?.instance?.state?.value?.busy == true ||
        promptsSlot.value.child?.instance?.isBusy == true
}

/** Owns global settings navigation and the saved provider list. */
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
            saved = {},
            back = { editorNavigation.dismiss() },
        ) as ProviderEditorComponent
    }
    private val promptsNavigation = SlotNavigation<Unit>()
    override val promptsSlot =
        childSlot(
            promptsNavigation,
            serializer = null,
            key = "PromptsSlot",
            handleBackButton = false,
        ) {
                _,
                context,
            ->
            DefaultPromptsComponent(context, back = {
                promptsNavigation.dismiss()
            }) as PromptsComponent
        }
    override fun onProviders() {
        if (!isBusy) state.value = state.value.copy(section = SettingsSection.ApiProviders)
    }
    override fun onPrompts() {
        if (!isBusy) promptsNavigation.activate(Unit)
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
        if (!isBusy && connection.id !in state.value.changingActive) {
            onProviders()
            editorNavigation.activate(connection)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onActiveChange(id: Long, active: Boolean) {
        if (isBusy || id in state.value.changingActive) return
        state.value = state.value.copy(changingActive = state.value.changingActive + id)
        scope.launch {
            try {
                useCases.setActive(id, active)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = state.value.copy(error = SettingsError.Save)
            } finally {
                state.value = state.value.copy(changingActive = state.value.changingActive - id)
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
                        state.value.copy(connections = it, loading = false, error = null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = state.value.copy(loading = false, error = SettingsError.Load)
            }
        }
    }
    override fun onBack() {
        val prompts = promptsSlot.value.child?.instance
        val editor = editorSlot.value.child?.instance
        when {
            prompts != null -> prompts.onBack()

            editor != null -> editor.onBack()

            state.value.section == SettingsSection.ApiProviders ->
                state.value = state.value.copy(section = SettingsSection.Overview)

            else -> back()
        }
    }
}
