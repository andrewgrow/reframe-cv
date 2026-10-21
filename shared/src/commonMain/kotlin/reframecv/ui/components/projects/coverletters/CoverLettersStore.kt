package reframecv.ui.components.projects.coverletters

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import reframecv.logging.ApplicationLogger
import reframecv.usecase.CoverLettersUseCases
import reframecv.usecase.VacanciesUseCases

/** Observes letters and vacancy names together, keeping failed reads retryable. */
internal interface CoverLettersStore : Store<Unit, CoverLettersState, Nothing>

internal fun createCoverLettersStore(
    factory: StoreFactory,
    projectId: Long,
    letters: CoverLettersUseCases,
    vacancies: VacanciesUseCases,
    logger: ApplicationLogger,
): CoverLettersStore = object :
    CoverLettersStore,
    Store<Unit, CoverLettersState, Nothing> by factory.create(
        name = "CoverLettersStore(projectId=$projectId)",
        initialState = CoverLettersState.Loading,
        executorFactory = { CoverLettersExecutor(projectId, letters, vacancies, logger) },
        reducer = Reducer<CoverLettersState, CoverLettersState> { it },
    ) {}

/** Keeps cover-letter browsing current without retaining stale vacancy links. */
private class CoverLettersExecutor(
    private val projectId: Long,
    private val letters: CoverLettersUseCases,
    private val vacancies: VacanciesUseCases,
    private val logger: ApplicationLogger,
) : CoroutineExecutor<Unit, Nothing, CoverLettersState, CoverLettersState, Nothing>() {
    private var observation: Job? = null

    @Suppress("TooGenericExceptionCaught")
    override fun executeIntent(intent: Unit) {
        observation?.cancel()
        dispatch(CoverLettersState.Loading)
        observation = scope.launch {
            try {
                combine(letters.observe(projectId), vacancies.observe(projectId)) {
                        records,
                        sources,
                    ->
                    CoverLettersState.Ready(records.sortedBy { it.id }, sources)
                }.collect { dispatch(it) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error("Load cover letters", error, mapOf("projectId" to projectId))
                dispatch(CoverLettersState.LoadFailed)
            }
        }
    }
}
