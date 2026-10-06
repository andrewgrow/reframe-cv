package reframecv.ui.components.projects.dashboard

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import reframecv.repository.CoverLettersRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository

internal class DashboardExecutor(
    private val projectId: Long,
    private val resumes: ResumesRepository,
    private val vacancies: VacanciesRepository,
    private val letters: CoverLettersRepository,
) : CoroutineExecutor<DashboardIntent, Nothing, DashboardState, DashboardState, Nothing>() {
    private var observation: Job? = null

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
            } catch (_: Exception) {
                dispatch(DashboardState.LoadFailed)
            }
        }
    }
}
