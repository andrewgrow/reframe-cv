package reframecv.ui.components.projects.vacancies

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.repository.VacanciesRepository

internal class VacanciesExecutor(
    private val projectId: Long,
    private val repository: VacanciesRepository,
) : CoroutineExecutor<VacanciesIntent, Nothing, VacanciesState, VacanciesState, Nothing>() {
    private var observation: Job? = null

    override fun executeIntent(intent: VacanciesIntent) {
        observation?.cancel()
        dispatch(VacanciesState.Loading)
        observation = scope.launch {
            try {
                repository.observeVacancies(projectId).collect {
                    dispatch(VacanciesState.Ready(it))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                dispatch(VacanciesState.LoadFailed)
            }
        }
    }
}
