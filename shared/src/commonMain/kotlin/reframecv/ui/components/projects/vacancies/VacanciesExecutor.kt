package reframecv.ui.components.projects.vacancies

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.VacanciesRepository
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft

internal class VacanciesExecutor(
    private val projectId: Long,
    private val repository: VacanciesRepository,
) : CoroutineExecutor<VacanciesIntent, Nothing, VacanciesState, VacanciesState, VacanciesLabel>() {
    private var observation: Job? = null

    private var saving: Job? = null

    override fun executeIntent(intent: VacanciesIntent) {
        when (intent) {
            VacanciesIntent.Load -> observe()
            is VacanciesIntent.Create -> save(intent.draft)
            is VacanciesIntent.Delete -> delete(intent.id)
            is VacanciesIntent.Update -> save(intent.draft, intent.vacancy)
        }
    }

    private fun save(draft: VacancyDraft, original: Vacancy? = null) {
        if (draft.title.isBlank() || saving?.isActive == true) return
        require(original == null || original.projectId == projectId)
        publish(VacanciesLabel.Saving)
        saving = scope.launch {
            try {
                if (original == null) {
                    repository.create(draft.toVacancy(projectId))
                } else {
                    repository.update(draft.applyTo(original))
                }
                publish(VacanciesLabel.Saved)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                publish(VacanciesLabel.SaveFailed)
            }
        }
    }

    private fun delete(id: Long) {
        if (saving?.isActive == true) return
        publish(VacanciesLabel.Deleting)
        saving = scope.launch {
            try {
                repository.delete(id)
                publish(VacanciesLabel.Deleted)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                publish(VacanciesLabel.DeleteFailed)
            }
        }
    }

    private fun observe() {
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
