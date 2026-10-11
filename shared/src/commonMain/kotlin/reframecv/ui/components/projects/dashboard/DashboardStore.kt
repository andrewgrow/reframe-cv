package reframecv.ui.components.projects.dashboard

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.logging.ApplicationLogger
import reframecv.repository.CoverLettersRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository

/** Gives all dashboard sections one source of workspace state. */
internal interface DashboardStore : Store<DashboardIntent, DashboardState, Nothing>

internal fun createDashboardStore(
    storeFactory: StoreFactory,
    projectId: Long,
    resumes: ResumesRepository,
    vacancies: VacanciesRepository,
    letters: CoverLettersRepository,
    logger: ApplicationLogger,
): DashboardStore = object :
    DashboardStore,
    Store<DashboardIntent, DashboardState, Nothing> by storeFactory.create(
        name = "DashboardStore(projectId=$projectId)",
        initialState = DashboardState.Loading,
        executorFactory = { DashboardExecutor(projectId, resumes, vacancies, letters, logger) },
        reducer = Reducer<DashboardState, DashboardState> { message -> message },
    ) { /* */ }
