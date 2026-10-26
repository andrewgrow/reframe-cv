package reframecv.providers.relevance

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.domain.models.providers.ProviderConnection
import reframecv.providers.AiProviderClient
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.providers.AiTextDocument
import reframecv.relevance.RunnerRelevanceException
import reframecv.relevance.RunnerRelevanceJson
import reframecv.relevance.RunnerRelevanceRepository
import reframecv.relevance.RunnerRelevanceScore

/** Sends one source snapshot and validates the provider's prompt-specific relevance score. */
class RemoteRunnerRelevanceRepository(
    private val provider: AiProviderClient,
    private val responseSchema: String,
) : RunnerRelevanceRepository {
    override suspend fun assess(
        sourceJson: String,
        connection: ProviderConnection,
        instruction: String,
    ): RunnerRelevanceScore {
        val response = provider.execute(
            AiRequest(
                instruction,
                responseSchema,
                emptyList(),
                connection,
                textDocuments = listOf(AiTextDocument("runner-result.json", sourceJson)),
            ),
        )
        currentCoroutineContext().ensureActive()
        return when (response) {
            is AiResponse.Failed -> throw RunnerRelevanceException(
                response.details ?: "The provider could not assess this result.",
            )

            is AiResponse.Generated -> decode(response.content)
        }
    }
    private fun decode(content: String): RunnerRelevanceScore = try {
        RunnerRelevanceJson.decode(content)
    } catch (error: IllegalArgumentException) {
        throw RunnerRelevanceException(
            "The response does not match the relevance score schema.",
            error,
        )
    }
}
