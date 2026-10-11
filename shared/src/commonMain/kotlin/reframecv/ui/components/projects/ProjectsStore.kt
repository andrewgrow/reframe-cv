package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.ui.components.projects.UiState.Loading

/** Gives project browsing and editing one source of state and operation results. */
internal interface ProjectsStore : Store<ProjectsIntent, UiState, ProjectsLabel>

internal fun createProjectsStore(
    storeFactory: StoreFactory,
    parentId: Long? = null,
    executorFactory: () -> ProjectsExecutor,
): ProjectsStore = object :
    ProjectsStore,
    Store<ProjectsIntent, UiState, ProjectsLabel> by storeFactory.create(
        name = "ProjectsStore(parentId=$parentId)",
        initialState = Loading,
        executorFactory = executorFactory,
        reducer = Reducer<UiState, UiState> { message -> message },
    ) { /* */ }
