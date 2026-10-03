package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.repository.ProjectsRepository

interface ProjectsExecutor : Executor<ProjectsIntent, Nothing, UiState, UiState, ProjectsLabel>

class RealProjectsExecutor(private val projectsRepository: ProjectsRepository) :
    CoroutineExecutor<ProjectsIntent, Nothing, UiState, UiState, ProjectsLabel>(),
    ProjectsExecutor {
    private var observation: Job? = null
    private var saving: Job? = null

    override fun executeIntent(intent: ProjectsIntent) {
        when (intent) {
            ProjectsIntent.LoadProjects -> observeProjects()
            is ProjectsIntent.CreateProject -> createProject(intent.name)
        }
    }

    private fun observeProjects() {
        observation?.cancel()
        observation = scope.launch {
            try {
                projectsRepository.observeProjects().collect { projects ->
                    val newState = if (projects.isEmpty()) {
                        UiState.NoProjects
                    } else {
                        UiState.Projects(
                            projects,
                        )
                    }
                    dispatch(newState)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                dispatch(UiState.LoadFailed)
            }
        }
    }

    private fun createProject(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || saving?.isActive == true) return
        publish(ProjectsLabel.Saving)
        saving = scope.launch {
            try {
                projectsRepository.createProject(trimmedName)
                publish(ProjectsLabel.Saved)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                publish(ProjectsLabel.SaveFailed)
            }
        }
    }
}
