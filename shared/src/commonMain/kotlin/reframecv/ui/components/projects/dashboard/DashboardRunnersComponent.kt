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
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.ui.components.settings.runners.DefaultRunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.context.AppComponentContext
import reframecv.usecase.SavedRunner

/** Holds the saved-runner picker independently of dashboard document loading. */
data class DashboardRunnersState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val runners: List<SavedRunner> = emptyList(),
)

/** Opens global runner results while retaining the dashboard's project as an import default. */
interface DashboardRunnersComponent {
    val state: Value<DashboardRunnersState>
    val execution: Value<ChildSlot<*, RunnerRunsComponent>>
    val isBusy: Boolean get() = execution.value.child?.instance?.isBusy == true
    fun open()
    fun select(id: Long)
    fun dismiss()
    fun onBack(): Boolean
}

/** Owns picker observation and the nested runner page without leaving the project dashboard. */
class DefaultDashboardRunnersComponent(context: AppComponentContext, private val projectId: Long) :
    DashboardRunnersComponent {
    private val useCases = context.dependencies.connectionRunnersUseCases
    private val logger = context.dependencies.logger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    private val navigation = SlotNavigation<ConnectionRunner>()
    override val state = MutableValue(DashboardRunnersState())
    override val execution = context.childSlot(
        navigation,
        serializer = null,
        key = "DashboardRunner",
        handleBackButton = false,
    ) { runner, childContext ->
        DefaultRunnerRunsComponent(
            childContext,
            runner,
            back = { navigation.dismiss() },
            preferredProjectId = projectId,
        ) as RunnerRunsComponent
    }
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun open() {
        if (execution.value.child != null || isBusy) return
        observation?.cancel()
        state.value = DashboardRunnersState(visible = true, loading = true)
        observation = scope.launch {
            try {
                useCases.observeSaved().collect {
                    state.value = DashboardRunnersState(visible = true, runners = it)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error("Load dashboard runners", error, mapOf("projectId" to projectId))
                state.value = DashboardRunnersState(visible = true, failed = true)
            }
        }
    }
    override fun select(id: Long) {
        if (!state.value.visible || state.value.loading || state.value.failed) return
        val choice = state.value.runners.find { it.runner.id == id } ?: return
        dismiss()
        navigation.activate(choice.runner.copy(active = choice.available))
    }
    override fun dismiss() {
        observation?.cancel()
        state.value = state.value.copy(visible = false)
    }
    override fun onBack(): Boolean {
        val child = execution.value.child?.instance
        val handled = child != null || state.value.visible
        if (child != null) {
            child.onBack()
        } else if (state.value.visible) {
            dismiss()
        }
        return handled
    }
}
