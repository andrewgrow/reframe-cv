package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor
import reframecv.domain.models.project.Project

class TestProjectsExecutor : ProjectsExecutor {
    private lateinit var callbacks: Executor.Callbacks<UiState, UiState, Nothing, ProjectsLabel>
    var loadCount = 0
        private set
    var isDisposed = false
        private set
    var lastIntent: ProjectsIntent? = null
        private set

    override fun init(callbacks: Executor.Callbacks<UiState, UiState, Nothing, ProjectsLabel>) {
        this.callbacks = callbacks
    }

    override fun executeIntent(intent: ProjectsIntent) {
        lastIntent = intent
        when (intent) {
            is ProjectsIntent.DeleteProject -> callbacks.onLabel(ProjectsLabel.Deleted)

            is ProjectsIntent.CreateProject -> callbacks.onLabel(
                ProjectsLabel.Created(
                    Project(id = 1, name = intent.name, createdAt = 0, updatedAt = 0),
                ),
            )

            is ProjectsIntent.UpdateProject -> callbacks.onLabel(
                ProjectsLabel.Saved,
            )

            ProjectsIntent.LoadProjects -> {
                loadCount++
                callbacks.onMessage(UiState.NoProjects)
            }
        }
    }

    override fun executeAction(action: Nothing) = Unit

    override fun dispose() {
        isDisposed = true
    }
}
