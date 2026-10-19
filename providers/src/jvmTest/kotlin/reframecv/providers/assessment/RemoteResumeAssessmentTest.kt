package reframecv.providers.assessment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import reframecv.assessment.ResumeAssessmentOutcome
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse

class RemoteResumeAssessmentTest {
    private val connection =
        ProviderConnection(
            type = ApiProviderType.OpenRouter,
            name = "Work",
            apiKey = "key",
            modelId = "model",
        )

    @Test
    fun sendsNamedJsonTextDocumentsAndValidatesTheResult() = runBlocking<Unit> {
        val requests = mutableListOf<AiRequest>()
        val client = object : AiProviderClient {
            override suspend fun execute(request: AiRequest): AiResponse {
                requests += request
                return AiResponse.Generated(
                    """{"score":78,"summary":"Kotlin matches; German is not evidenced."}""",
                )
            }
        }
        val repository = RemoteResumeAssessmentRepository(client, resumeAssessmentSchema())
        val result = repository.assess(
            "{\"resume\":1}",
            "{\"vacancy\":2}",
            connection,
            "Assess these documents",
        )
        assertEquals(78, (result as ResumeAssessmentOutcome.Assessed).result.score)
        val request = requests.single()
        assertTrue(request.documents.isEmpty())
        assertEquals(listOf("resume.json", "vacancy.json"), request.textDocuments.map { it.name })
        assertEquals(
            listOf("{\"resume\":1}", "{\"vacancy\":2}"),
            request.textDocuments.map {
                it.content
            },
        )
        assertEquals("Assess these documents", request.instruction)
    }

    @Test
    fun errorsAndInvalidResponsesDoNotProduceAssessmentsAndCancellationPropagates() =
        runBlocking<Unit> {
            var response: AiResponse = AiResponse.Failed(
                AiProviderError.Unavailable,
                "API key expired.",
            )
            var cancel = false
            val repository = RemoteResumeAssessmentRepository(
                object : AiProviderClient {
                    override suspend fun execute(request: AiRequest): AiResponse {
                        if (cancel) throw CancellationException("Closed")
                        return response
                    }
                },
                resumeAssessmentSchema(),
            )
            assertEquals(
                ResumeAssessmentOutcome.Failed("API key expired."),
                repository.assess("{}", "{}", connection, "Assess"),
            )
            response = AiResponse.Generated("""{"score":200,"summary":"Invalid"}""")
            assertTrue(
                repository.assess(
                    "{}",
                    "{}",
                    connection,
                    "Assess",
                ) is ResumeAssessmentOutcome.Failed,
            )
            cancel = true
            assertFailsWith<CancellationException> {
                repository.assess("{}", "{}", connection, "Assess")
            }
        }
}
