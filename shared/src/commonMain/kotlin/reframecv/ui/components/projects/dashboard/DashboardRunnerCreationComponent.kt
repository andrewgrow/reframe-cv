package reframecv.ui.components.projects.dashboard

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
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.ui.components.settings.runners.DefaultRunnerEditorComponent
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.components.settings.runners.RunnerEditorComponent
import reframecv.ui.context.AppComponentContext

/** Lists saved connections that support creating a search runner. */
data class DashboardRunnerCreationState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val connections: List<ProviderConnection> = emptyList(),
)

/** Creates a runner from the dashboard without starting a remote run. */
interface DashboardRunnerCreationComponent {
    val state: Value<DashboardRunnerCreationState>
    val editor: Value<ChildSlot<*, RunnerEditorComponent>>
    val isBusy: Boolean get() = editor.value.child?.instance?.isBusy == true
    fun open()
    fun select(connectionId: Long)
    fun dismiss()
    fun onBack(): Boolean
}

/** Keeps connection selection and the new runner's draft local to the dashboard. */
class DefaultDashboardRunnerCreationComponent(context: AppComponentContext) :
    DashboardRunnerCreationComponent {
    private val useCases = context.dependencies.providerSettingsUseCases
    private val logger = context.dependencies.logger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    private val navigation = SlotNavigation<Long>()
    override val state = MutableValue(DashboardRunnerCreationState())
    override val editor = context.childSlot(
        navigation,
        serializer = null,
        key = "DashboardNewRunner",
        handleBackButton = false,
    ) { id, childContext ->
        DefaultRunnerEditorComponent(childContext, RunnerDraft.newRunner(id)) {
            navigation.dismiss()
        } as RunnerEditorComponent
    }
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun open() {
        if (editor.value.child != null) return
        observation?.cancel()
        state.value = DashboardRunnerCreationState(visible = true, loading = true)
        observation = scope.launch {
            try {
                useCases.observe().collect { connections ->
                    state.value = DashboardRunnerCreationState(
                        visible = true,
                        connections = connections.filter { it.type == ApiProviderType.Apify }
                            .sortedBy { it.id },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error("Load connections for new dashboard runner", error, emptyMap())
                state.value = DashboardRunnerCreationState(visible = true, failed = true)
            }
        }
    }
    override fun select(connectionId: Long) {
        val current = state.value
        if (!current.visible || current.loading || current.failed) return
        if (current.connections.none { it.id == connectionId }) return
        dismiss()
        navigation.activate(connectionId)
    }
    override fun dismiss() {
        observation?.cancel()
        state.value = state.value.copy(visible = false)
    }
    override fun onBack(): Boolean {
        val child = editor.value.child?.instance
        val handled = child != null || state.value.visible
        if (child != null) child.onBack() else dismiss()
        return handled
    }
}
