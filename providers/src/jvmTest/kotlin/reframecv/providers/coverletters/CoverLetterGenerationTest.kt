package reframecv.providers.coverletters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import reframecv.coverletters.CoverLetterGenerationOutcome
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse

class CoverLetterGenerationTest {
    private val connection =
        ProviderConnection(
            type = ApiProviderType.OpenRouter,
            name = "Work",
            configuration = OpenRouterConfiguration(
                apiKey = "key",
                modelId = "model",
            ),
        )

    @Test
    fun sendsTwoPortableDocumentsAndDecodesOnlyNonblankStringContent() = runBlocking<Unit> {
        val requests = mutableListOf<AiRequest>()
        var response = """{"content":"Dear team,\\nKotlin experience."}"""
        val repository = RemoteCoverLetterGenerationRepository(
            object : AiProviderClient {
                override suspend fun execute(request: AiRequest): AiResponse {
                    requests += request
                    return AiResponse.Generated(response)
                }
            },
            coverLetterGenerationSchema(),
        )
        assertIs<CoverLetterGenerationOutcome.Generated>(
            repository.generate("resume", "vacancy", connection, "Write"),
        )
        val request = requests.single()
        assertEquals(listOf("resume.json", "vacancy.json"), request.textDocuments.map { it.name })
        assertEquals(listOf("resume", "vacancy"), request.textDocuments.map { it.content })
        assertTrue(request.documents.isEmpty())
        assertEquals(coverLetterGenerationSchema(), request.responseSchema)
        for (invalid in listOf(
            "not json",
            "[]",
            "{}",
            "null",
            """{"content":null}""",
            """{"content":1}""",
            """{"content":" "}""",
            """{"content":"Text","extra":true}""",
        )) {
            response = invalid
            assertIs<CoverLetterGenerationOutcome.Failed>(
                repository.generate("r", "v", connection, "Write"),
            )
        }
    }

    @Test
    fun preservesProviderErrorsAndPropagatesCancellation() = runBlocking<Unit> {
        var cancel = false
        val repository = RemoteCoverLetterGenerationRepository(
            object : AiProviderClient {
                override suspend fun execute(request: AiRequest): AiResponse {
                    if (cancel) throw CancellationException("Closed")
                    return AiResponse.Failed(AiProviderError.Unavailable, "API key expired.")
                }
            },
            coverLetterGenerationSchema(),
        )
        assertEquals(
            CoverLetterGenerationOutcome.Failed("API key expired."),
            repository.generate("r", "v", connection, "Write"),
        )
        cancel = true
        assertFailsWith<CancellationException> {
            repository.generate("r", "v", connection, "Write")
        }
    }
}
