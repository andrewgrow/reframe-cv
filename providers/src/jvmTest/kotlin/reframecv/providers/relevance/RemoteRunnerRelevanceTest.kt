package reframecv.providers.relevance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.relevance.RunnerRelevanceException

class RemoteRunnerRelevanceTest {
    private val connection = ProviderConnection(
        type = ApiProviderType.OpenRouter,
        name = "Example AI",
        configuration = OpenRouterConfiguration(
            apiKey = "fictional-key",
            modelId = "example/model",
        ),
    )
    private val source = """{"title":"Android developer","unknown":{"field":true}}"""

    @Test
    fun sendsOnlyTheOriginalNamedRowAndTheSelectedInstructionWithTheSchema() = runBlocking<Unit> {
        val requests = mutableListOf<AiRequest>()
        val repository = RemoteRunnerRelevanceRepository(
            object : AiProviderClient {
                override suspend fun execute(request: AiRequest): AiResponse {
                    requests += request
                    return AiResponse.Generated("""{"score":92}""")
                }
            },
            runnerRelevanceSchema(),
        )
        assertEquals(92, repository.assess(source, connection, "Find Android roles").score)
        val request = requests.single()
        assertEquals("Find Android roles", request.instruction)
        assertEquals(runnerRelevanceSchema(), request.responseSchema)
        assertEquals("runner-result.json", request.textDocuments.single().name)
        assertEquals(source, request.textDocuments.single().content)
        assertTrue(request.documents.isEmpty())
    }

    @Test
    fun rejectsInvalidScoresAndPreservesProviderDetailsAndCancellation() = runBlocking<Unit> {
        var response: AiResponse = AiResponse.Generated("""{"score":200}""")
        var cancel = false
        val repository = RemoteRunnerRelevanceRepository(
            object : AiProviderClient {
                override suspend fun execute(request: AiRequest): AiResponse {
                    if (cancel) throw CancellationException("Closed")
                    return response
                }
            },
            runnerRelevanceSchema(),
        )
        assertFailsWith<RunnerRelevanceException> {
            repository.assess(source, connection, "Assess")
        }
        response = AiResponse.Failed(AiProviderError.Unavailable, "API key expired.")
        assertEquals(
            "API key expired.",
            assertFailsWith<RunnerRelevanceException> {
                repository.assess(source, connection, "Assess")
            }.message,
        )
        cancel = true
        assertFailsWith<CancellationException> { repository.assess(source, connection, "Assess") }
    }
}
