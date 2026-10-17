package reframecv.ui.components.projects.dashboard

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.logging.ApplicationLogger
import reframecv.usecase.ObserveDashboardUseCase

/** Keeps dashboard previews current as workspace documents change. */
internal class DashboardExecutor(
    private val projectId: Long,
    private val observeDashboard: ObserveDashboardUseCase,
    private val logger: ApplicationLogger,
) : CoroutineExecutor<DashboardIntent, Nothing, DashboardState, DashboardState, Nothing>() {
    private var observation: Job? = null

    // Repository implementations can fail with different exceptions; keep the screen usable.
    @Suppress("TooGenericExceptionCaught")
    override fun executeIntent(intent: DashboardIntent) {
        observation?.cancel()
        dispatch(DashboardState.Loading)
        observation = scope.launch {
            try {
                observeDashboard(projectId).collect {
                    dispatch(DashboardState.Ready(it.resumes, it.vacancies, it.coverLetters))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                logger.error("Load dashboard", error, mapOf("projectId" to projectId))
                dispatch(DashboardState.LoadFailed)
            }
        }
    }
}
