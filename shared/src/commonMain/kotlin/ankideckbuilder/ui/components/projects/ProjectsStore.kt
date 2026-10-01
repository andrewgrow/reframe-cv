package ankideckbuilder.ui.components.projects

import ankideckbuilder.ui.components.projects.UiState.NoProjects
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory

internal interface ProjectsStore : Store<ProjectsIntent, UiState, Nothing>

private const val PROJECTS_STORE_NAME = "ProjectsStore"
internal fun createProjectsStore(
    storeFactory: StoreFactory,
    executorFactory: () -> ProjectsExecutor = ::RealProjectsExecutor,
): ProjectsStore = object :
    ProjectsStore,
    Store<ProjectsIntent, UiState, Nothing> by storeFactory.create(
        name = PROJECTS_STORE_NAME,
        initialState = NoProjects,
        executorFactory = executorFactory,
        reducer = Reducer<UiState, UiState> { message -> message },
    ) { /* */ }
