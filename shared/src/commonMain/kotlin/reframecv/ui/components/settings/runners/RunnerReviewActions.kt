package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import reframecv.ui.context.AppComponentContext

/** Identifies explicit review decisions independently of table presentation. */
enum class RunnerResultAction { Convert, Dismiss, Restore }

/** Keeps project selection and transactional result actions separate from remote polling. */
internal class RunnerReviewActions(
    context: AppComponentContext,
    private val state: MutableValue<RunnerRunsState>,
    private val scope: CoroutineScope,
    private val reload: suspend () -> Unit,
) {
    private val useCases = context.dependencies.runnerResultReviewUseCases
    private val logger = context.dependencies.logger
    fun act(id: Long, action: RunnerResultAction) {
        if (state.value.working || state.value.reading) return
        val view = state.value.view ?: return
        require(view.page?.savedResults?.any { it.id == id } == true)
        perform {
            if (action == RunnerResultAction.Convert) {
                state.value =
                    state.value.copy(importResultId = id, projects = useCases.projects().first())
            } else {
                useCases.review(view.run.id, id, action == RunnerResultAction.Dismiss)
                reload()
            }
        }
    }
    fun project(id: Long?) {
        if (state.value.working) return
        val result = state.value.importResultId
        val run = state.value.view?.run
        if (id == null) {
            state.value = state.value.copy(importResultId = null, error = null)
        } else if (result != null && run != null) {
            require(state.value.projects.any { it.id == id })
            perform {
                useCases.import(run.id, result, id)
                state.value = state.value.copy(importResultId = null, notice = "Vacancy created.")
                reload()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun perform(block: suspend () -> Unit) {
        state.value = state.value.copy(working = true, error = null, notice = null)
        scope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error(
                    "Review runner result",
                    error,
                    mapOf("runId" to state.value.view?.run?.id),
                )
                state.value =
                    state.value.copy(
                        error = "Could not process this result. Check the data and try again.",
                    )
            } finally {
                state.value = state.value.copy(working = false)
            }
        }
    }
}
