package reframecv.ui.components.projects.vacancies

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.logging.ApplicationLogger
import reframecv.repository.VacanciesRepository

/** Gives vacancy browsing and editing one source of state and operation results. */
internal interface VacanciesStore : Store<VacanciesIntent, VacanciesState, VacanciesLabel>

internal fun createVacanciesStore(
    storeFactory: StoreFactory,
    projectId: Long,
    repository: VacanciesRepository,
    logger: ApplicationLogger,
): VacanciesStore = object :
    VacanciesStore,
    Store<VacanciesIntent, VacanciesState, VacanciesLabel> by storeFactory.create(
        name = "VacanciesStore(projectId=$projectId)",
        initialState = VacanciesState.Loading,
        executorFactory = { VacanciesExecutor(projectId, repository, logger) },
        reducer = Reducer<VacanciesState, VacanciesState> { message -> message },
    ) { /* */ }
