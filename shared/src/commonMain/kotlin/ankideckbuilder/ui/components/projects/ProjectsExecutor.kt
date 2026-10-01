package ankideckbuilder.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor

interface ProjectsExecutor : Executor<ProjectsIntent, Nothing, UiState, UiState, Nothing>

class RealProjectsExecutor :
    CoroutineExecutor<ProjectsIntent, Nothing, UiState, UiState, Nothing>(),
    ProjectsExecutor {
    override fun executeIntent(intent: ProjectsIntent) {
        when (intent) {
            // Until project loading is implemented, the result is always empty.
            ProjectsIntent.LoadProjects -> dispatch(UiState.NoProjects)
        }
    }
}
