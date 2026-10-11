package reframecv.ui.components.projects.dashboard

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import reframecv.logging.ApplicationLogger
import reframecv.repository.CoverLettersRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository

/** Keeps dashboard previews current as workspace documents change. */
internal class DashboardExecutor(
    private val projectId: Long,
    private val resumes: ResumesRepository,
    private val vacancies: VacanciesRepository,
    private val letters: CoverLettersRepository,
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
                combine(
                    resumes.observeResumes(projectId),
                    vacancies.observeVacancies(projectId),
                    letters.observeCoverLetters(projectId),
                ) { resumes, vacancies, letters ->
                    DashboardState.Ready(resumes, vacancies, letters)
                }.collect { dispatch(it) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                logger.error("Load dashboard", error, mapOf("projectId" to projectId))
                dispatch(DashboardState.LoadFailed)
            }
        }
    }
}
