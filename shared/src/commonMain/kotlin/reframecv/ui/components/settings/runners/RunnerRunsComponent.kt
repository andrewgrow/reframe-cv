package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnResume
import com.arkivanov.essenty.lifecycle.doOnStop
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import reframecv.domain.models.project.Project
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.ui.context.AppComponentContext
import reframecv.usecase.RunnerRunView

/** Keeps history and paged remote results independently of the runner's editable draft. */
data class RunnerRunsState(
    val runner: ConnectionRunner,
    val runs: List<ConnectionRun> = emptyList(),
    val loadingHistory: Boolean = true,
    val historyFailed: Boolean = false,
    val starting: Boolean = false,
    val reading: Boolean = false,
    val view: RunnerRunView? = null,
    val error: String? = null,
    val working: Boolean = false,
    val importResultId: Long? = null,
    val projects: List<Project> = emptyList(),
    val notice: String? = null,
    val preferredProjectId: Long? = null,
    val openingSettings: Boolean = false,
    val filter: RunnerResultFilter = RunnerResultFilter(),
)

/** Starts explicit paid runs and resumes status/result reads for previously saved IDs. */
interface RunnerRunsComponent {
    val state: Value<RunnerRunsState>
    val settings: RunnerSettingsComponent
    val isBusy: Boolean get() = state.value.starting || state.value.working || settings.isBusy
    fun onLoadMore()
    fun onResultAction(id: Long, action: RunnerResultAction)
    fun onProject(id: Long?)
    fun onStart()
    fun onSelect(id: Long)
    fun onRefresh()
    fun onFilter(filter: RunnerResultFilter)
    fun onPage(offset: Int)
    fun onRetryHistory()
    fun onBack()
}

/** Polls only while visible; leaving cancels local requests without aborting remote work. */
class DefaultRunnerRunsComponent(
    context: AppComponentContext,
    runner: ConnectionRunner,
    private val back: () -> Unit,
    private val awaitPoll: suspend () -> Unit = { delay(POLL_INTERVAL_MS) },
    preferredProjectId: Long? = null,
) : RunnerRunsComponent {
    private val useCases = context.dependencies.runnerExecutionUseCases
    private val logger = context.dependencies.logger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    private var request: Job? = null
    override val state =
        MutableValue(RunnerRunsState(runner, preferredProjectId = preferredProjectId))
    override val settings: RunnerSettingsComponent =
        DefaultRunnerSettingsComponent(
            context,
            state,
            scope,
            pause = {
                request?.cancel()
                state.value = state.value.copy(reading = false)
            },
            resume = {
                val run = state.value.view?.run
                val pending = run?.state == ConnectionRunState.Pending ||
                    run?.state == ConnectionRunState.Running
                if (pending && state.value.runner.active) onRefresh()
            },
        )
    private val review = RunnerReviewActions(context, state, scope) {
        val view = state.value.view ?: return@RunnerReviewActions
        state.value = state.value.copy(
            view = useCases.dataset.page(
                state.value.runner.id,
                view.run.id,
                view.page?.offset ?: 0,
                state.value.filter,
            ),
        )
    }
    override fun onResultAction(id: Long, action: RunnerResultAction) = review.act(id, action)
    override fun onProject(id: Long?) = review.project(id)
    override fun onLoadMore() {
        val view = state.value.view ?: return
        if (state.value.reading || state.value.working) return
        request = scope.launch { read(view.run.id, view.page?.offset ?: 0, more = true) }
    }
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
        context.lifecycle.doOnStop {
            if (!state.value.starting && !state.value.working) {
                request?.cancel()
                state.value = state.value.copy(reading = false)
            }
        }
        context.lifecycle.doOnResume {
            val run = state.value.view?.run
            val pending =
                run?.state == ConnectionRunState.Pending || run?.state == ConnectionRunState.Running
            if (pending && !isBusy && settings.editor.value.child == null) onRefresh()
        }
        onRetryHistory()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRetryHistory() {
        observation?.cancel()
        state.value = state.value.copy(loadingHistory = true, historyFailed = false)
        observation = scope.launch {
            try {
                useCases.observe(state.value.runner.id).collect { runs ->
                    state.value = state.value.copy(
                        runs = runs.sortedByDescending { it.id },
                        loadingHistory = false,
                        historyFailed = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error(
                    "Load runner history",
                    error,
                    mapOf("runnerId" to state.value.runner.id),
                )
                state.value = state.value.copy(loadingHistory = false, historyFailed = true)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onStart() {
        if (isBusy || !state.value.runner.active) return
        request?.cancel()
        state.value = state.value.copy(starting = true, reading = false, error = null, view = null)
        request = scope.launch {
            try {
                val run = useCases.start(state.value.runner.id)
                state.value = state.value.copy(starting = false, view = RunnerRunView(run, null))
                read(run.id, 0)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                runnerReadFailure(state, logger, "Start runner", error, starting = true)
            }
        }
    }
    override fun onSelect(id: Long) {
        if (state.value.starting || state.value.working) return
        val run = state.value.runs.find { it.id == id } ?: return
        request?.cancel()
        state.value = state.value.copy(view = RunnerRunView(run, null), error = null)
        request = scope.launch {
            read(
                run.id,
                0,
                local =
                    run.state == ConnectionRunState.Succeeded && run.datasetId != null &&
                        (run.nextResultOffset > 0 || run.resultsComplete),
            )
        }
    }
    override fun onRefresh() {
        if (state.value.starting || state.value.working) return
        val run = state.value.view?.run ?: return
        request?.cancel()
        request = scope.launch { read(run.id, state.value.view?.page?.offset ?: 0) }
    }
    override fun onFilter(filter: RunnerResultFilter) {
        if (isBusy || state.value.reading) return
        val run = state.value.view?.run ?: return
        request?.cancel()
        state.value = state.value.copy(filter = filter)
        request = scope.launch { read(run.id, 0, local = true) }
    }
    override fun onPage(offset: Int) {
        if (!state.value.reading && !state.value.working && offset >= 0) {
            val run = state.value.view?.run ?: return
            request?.cancel()
            request = scope.launch { read(run.id, offset, local = true) }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun read(id: Long, offset: Int, local: Boolean = false, more: Boolean = false) {
        state.value = state.value.copy(reading = true, error = null)
        try {
            while (currentCoroutineContext().isActive) {
                val view = when {
                    more -> useCases.dataset.loadMore(
                        state.value.runner.id,
                        id,
                        offset,
                        state.value.filter,
                    )

                    local -> useCases.dataset.page(
                        state.value.runner.id,
                        id,
                        offset,
                        state.value.filter,
                    )

                    else -> useCases.inspect(state.value.runner.id, id, offset, state.value.filter)
                }
                state.value = state.value.copy(view = view, reading = false)
                val pending =
                    view.run.state == ConnectionRunState.Pending ||
                        view.run.state == ConnectionRunState.Running
                if (!pending || view.run.externalId == null) break
                awaitPoll()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            runnerReadFailure(state, logger, "Read runner results", error)
        }
    }
    override fun onBack() {
        if (isBusy || settings.onBack()) return
        if (state.value.importResultId != null) {
            review.project(null)
            return
        }
        request?.cancel()
        if (state.value.view !=
            null
        ) {
            state.value = state.value.copy(view = null, reading = false, error = null)
        } else {
            back()
        }
    }
}
private const val POLL_INTERVAL_MS = 5_000L
