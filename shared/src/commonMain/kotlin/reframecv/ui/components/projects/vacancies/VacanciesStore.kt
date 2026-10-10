package reframecv.ui.components.projects.vacancies

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.repository.VacanciesRepository

internal interface VacanciesStore : Store<VacanciesIntent, VacanciesState, VacanciesLabel>

internal fun createVacanciesStore(
    storeFactory: StoreFactory,
    projectId: Long,
    repository: VacanciesRepository,
): VacanciesStore = object :
    VacanciesStore,
    Store<VacanciesIntent, VacanciesState, VacanciesLabel> by storeFactory.create(
        name = "VacanciesStore",
        initialState = VacanciesState.Loading,
        executorFactory = { VacanciesExecutor(projectId, repository) },
        reducer = Reducer<VacanciesState, VacanciesState> { message -> message },
    ) { /* */ }
