package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.ui.context.AppComponentContext

/** Opens the selected runner's configuration while retaining its results page. */
interface RunnerSettingsComponent {
    val editor: Value<ChildSlot<*, RunnerEditorComponent>>
    val isBusy: Boolean
    fun open()
    fun onBack(): Boolean
}

/** Loads fresh runner settings and refreshes availability when returning to results. */
internal class DefaultRunnerSettingsComponent(
    context: AppComponentContext,
    private val state: MutableValue<RunnerRunsState>,
    private val scope: CoroutineScope,
    private val pause: () -> Unit,
    private val resume: () -> Unit,
) : RunnerSettingsComponent {
    private val runners = context.dependencies.connectionRunnersUseCases
    private val logger = context.dependencies.logger
    private val navigation = SlotNavigation<ConnectionRunner>()
    override val editor = context.childSlot(
        navigation,
        serializer = null,
        key = "RunnerSettings",
        handleBackButton = false,
    ) { runner, childContext ->
        DefaultRunnerEditorComponent(
            childContext,
            runner,
            back = {
                navigation.dismiss()
                refreshRunner()
            },
        ) as RunnerEditorComponent
    }
    override val isBusy: Boolean get() = state.value.openingSettings ||
        editor.value.child?.instance?.isBusy == true

    @Suppress("TooGenericExceptionCaught")
    override fun open() {
        if (state.value.starting || state.value.working || isBusy) return
        if (editor.value.child != null) return
        pause()
        state.value = state.value.copy(openingSettings = true, error = null)
        scope.launch {
            try {
                val saved = runners.observeSaved().first()
                    .firstOrNull { it.runner.id == state.value.runner.id }
                if (saved == null) {
                    state.value = state.value.copy(error = "Runner no longer exists.")
                } else {
                    navigation.activate(saved.runner)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error(
                    "Open runner settings",
                    error,
                    mapOf("runnerId" to state.value.runner.id),
                )
                state.value =
                    state.value.copy(error = "Could not open runner settings. Try again.")
            } finally {
                state.value = state.value.copy(openingSettings = false)
                if (editor.value.child == null) resume()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun refreshRunner() {
        state.value = state.value.copy(openingSettings = true)
        scope.launch {
            try {
                val saved = runners.observeSaved().first()
                    .firstOrNull { it.runner.id == state.value.runner.id }
                val runner = saved?.runner?.copy(active = saved.available)
                    ?: state.value.runner.copy(active = false)
                state.value = state.value.copy(runner = runner)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error(
                    "Reload runner settings",
                    error,
                    mapOf("runnerId" to state.value.runner.id),
                )
                state.value = state.value.copy(error = "Could not reload runner settings.")
            } finally {
                state.value = state.value.copy(openingSettings = false)
                resume()
            }
        }
    }
    override fun onBack(): Boolean {
        val child = editor.value.child?.instance
        if (child != null) child.onBack()
        return child != null
    }
}
