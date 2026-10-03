package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor

class TestProjectsExecutor : ProjectsExecutor {
    private lateinit var callbacks: Executor.Callbacks<UiState, UiState, Nothing, ProjectsLabel>
    var loadCount = 0
        private set
    var isDisposed = false
        private set

    override fun init(callbacks: Executor.Callbacks<UiState, UiState, Nothing, ProjectsLabel>) {
        this.callbacks = callbacks
    }

    override fun executeIntent(intent: ProjectsIntent) {
        when (intent) {
            is ProjectsIntent.CreateProject -> callbacks.onLabel(ProjectsLabel.Saved)

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
