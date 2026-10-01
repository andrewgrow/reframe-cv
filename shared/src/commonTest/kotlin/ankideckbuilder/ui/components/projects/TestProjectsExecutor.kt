package ankideckbuilder.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor

class TestProjectsExecutor : ProjectsExecutor {
    private lateinit var callbacks: Executor.Callbacks<UiState, UiState, Nothing, Nothing>
    var loadCount = 0
        private set
    var isDisposed = false
        private set

    override fun init(callbacks: Executor.Callbacks<UiState, UiState, Nothing, Nothing>) {
        this.callbacks = callbacks
    }

    override fun executeIntent(intent: ProjectsIntent) {
        when (intent) {
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
