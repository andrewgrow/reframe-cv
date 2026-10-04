package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.repository.ProjectsRepository

interface ProjectsExecutor : Executor<ProjectsIntent, Nothing, UiState, UiState, ProjectsLabel>

class RealProjectsExecutor(
    private val projectsRepository: ProjectsRepository,
    private val parentId: Long? = null,
) : CoroutineExecutor<ProjectsIntent, Nothing, UiState, UiState, ProjectsLabel>(),
    ProjectsExecutor {
    private var observation: Job? = null
    private var saving: Job? = null

    override fun executeIntent(intent: ProjectsIntent) {
        when (intent) {
            ProjectsIntent.LoadProjects -> observeProjects()

            is ProjectsIntent.CreateProject -> saveProject(intent.name) {
                projectsRepository.createProject(it, parentId)
            }

            is ProjectsIntent.UpdateProject -> saveProject(intent.name) {
                projectsRepository.updateProject(intent.id, it)
            }

            is ProjectsIntent.DeleteProject -> deleteProject(intent.id)
        }
    }

    private fun observeProjects() {
        observation?.cancel()
        observation = scope.launch {
            try {
                projectsRepository.observeProjects(parentId).collect { projects ->
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

    private fun saveProject(name: String, save: suspend (String) -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || saving?.isActive == true) return
        publish(ProjectsLabel.Saving)
        saving = scope.launch {
            try {
                save(trimmedName)
                publish(ProjectsLabel.Saved)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                publish(ProjectsLabel.SaveFailed)
            }
        }
    }

    private fun deleteProject(id: Long) {
        if (saving?.isActive == true) return
        publish(ProjectsLabel.Deleting)
        saving = scope.launch {
            try {
                projectsRepository.deleteProject(id)
                publish(ProjectsLabel.Deleted)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                publish(ProjectsLabel.DeleteFailed)
            }
        }
    }
}
