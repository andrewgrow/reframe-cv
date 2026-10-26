package reframecv.usecase

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.domain.models.providers.RunnerResult
import reframecv.relevance.RunnerRelevanceException
import reframecv.relevance.RunnerRelevanceRepository
import reframecv.repository.RunnerResultsRepository

/** Scores one saved source row and atomically replaces its last score and exact prompt text. */
class AssessRunnerResultUseCase(
    private val repository: RunnerRelevanceRepository,
    private val providers: ProviderSettingsUseCases,
    private val results: RunnerResultsRepository,
) {
    suspend operator fun invoke(
        runId: Long,
        resultId: Long,
        connectionId: Long,
        instruction: String,
    ): RunnerResult {
        require(instruction.isNotBlank())
        val source = results.get(resultId)
            ?.takeIf { it.runId == runId }
            ?: throw RunnerRelevanceException("This runner result no longer exists.")
        val connection = providers.requireUsableConnection(connectionId)
        val score = repository.assess(source.rawResultAsJson, connection, instruction)
        currentCoroutineContext().ensureActive()
        providers.requireUsableConnection(connectionId)
        persist(source, score.score, instruction)
        return source.copy(relevanceScore = score.score, relevancePrompt = instruction)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun persist(source: RunnerResult, score: Int, prompt: String) {
        try {
            results.setRelevance(source.id, source.runId, score, prompt)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw RunnerRelevanceException(
                "The relevance score could not be saved. Please retry.",
                error,
            )
        }
    }
}
