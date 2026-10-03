package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.ui.components.projects.UiState.NoProjects

internal interface ProjectsStore : Store<ProjectsIntent, UiState, ProjectsLabel>

private const val PROJECTS_STORE_NAME = "ProjectsStore"
internal fun createProjectsStore(
    storeFactory: StoreFactory,
    executorFactory: () -> ProjectsExecutor,
): ProjectsStore = object :
    ProjectsStore,
    Store<ProjectsIntent, UiState, ProjectsLabel> by storeFactory.create(
        name = PROJECTS_STORE_NAME,
        initialState = NoProjects,
        executorFactory = executorFactory,
        reducer = Reducer<UiState, UiState> { message -> message },
    ) { /* */ }
