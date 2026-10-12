package reframecv.ui.components.projects.resumes

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.logging.ApplicationLogger
import reframecv.repository.ResumesRepository

/** Gives resume browsing a lifecycle-bound source of observable state. */
internal interface ResumesStore : Store<ResumesIntent, ResumesState, Nothing>

internal fun createResumesStore(
    storeFactory: StoreFactory,
    projectId: Long,
    repository: ResumesRepository,
    logger: ApplicationLogger,
): ResumesStore = object :
    ResumesStore,
    Store<ResumesIntent, ResumesState, Nothing> by storeFactory.create(
        name = "ResumesStore(projectId=$projectId)",
        initialState = ResumesState.Loading,
        executorFactory = { ResumesExecutor(projectId, repository, logger) },
        reducer = Reducer<ResumesState, ResumesState> { message -> message },
    ) { /* */ }
