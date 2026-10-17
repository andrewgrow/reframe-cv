package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.logging.ApplicationLogger
import reframecv.usecase.ProjectsUseCases

/** Lets the project store use an interchangeable handler for loading and editing. */
interface ProjectsExecutor : Executor<ProjectsIntent, Nothing, UiState, UiState, ProjectsLabel>

/** Keeps project lists current and gives the editor feedback when changes succeed or fail. */
class RealProjectsExecutor(
    private val useCases: ProjectsUseCases,
    private val parentId: Long? = null,
    private val logger: ApplicationLogger = ApplicationLogger.None,
) : CoroutineExecutor<ProjectsIntent, Nothing, UiState, UiState, ProjectsLabel>(),
    ProjectsExecutor {
    private var observation: Job? = null
    private var saving: Job? = null

    override fun executeIntent(intent: ProjectsIntent) {
        when (intent) {
            ProjectsIntent.LoadProjects -> observeProjects()

            is ProjectsIntent.CreateProject -> saveProject(
                intent.name,
                "Create project",
                mapOf("parentId" to parentId),
            ) {
                ProjectsLabel.Created(useCases.create(it, parentId))
            }

            is ProjectsIntent.UpdateProject -> saveProject(
                intent.name,
                "Update project",
                mapOf("projectId" to intent.id),
            ) {
                useCases.rename(intent.id, it)
                ProjectsLabel.Saved
            }

            is ProjectsIntent.DeleteProject -> deleteProject(intent.id)
        }
    }

    // Repository implementations can fail with different exceptions; keep the screen usable.
    @Suppress("TooGenericExceptionCaught")
    private fun observeProjects() {
        observation?.cancel()
        dispatch(UiState.Loading)
        observation = scope.launch {
            try {
                useCases.observeChildren(parentId).collect { projects ->
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
            } catch (error: Exception) {
                logger.error("Load projects", error, mapOf("parentId" to parentId))
                dispatch(UiState.LoadFailed)
            }
        }
    }

    // Repository implementations can fail with different exceptions; keep the screen usable.
    @Suppress("TooGenericExceptionCaught")
    private fun saveProject(
        name: String,
        operation: String,
        context: Map<String, Any?>,
        save: suspend (String) -> ProjectsLabel,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || saving?.isActive == true) return
        publish(ProjectsLabel.Saving)
        saving = scope.launch {
            try {
                publish(save(trimmedName))
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                logger.error(operation, error, context)
                publish(ProjectsLabel.SaveFailed)
            }
        }
    }

    // Repository implementations can fail with different exceptions; keep the screen usable.
    @Suppress("TooGenericExceptionCaught")
    private fun deleteProject(id: Long) {
        if (saving?.isActive == true) return
        publish(ProjectsLabel.Deleting)
        saving = scope.launch {
            try {
                useCases.delete(id)
                publish(ProjectsLabel.Deleted)
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                logger.error("Delete project", error, mapOf("projectId" to id))
                publish(ProjectsLabel.DeleteFailed)
            }
        }
    }
}
