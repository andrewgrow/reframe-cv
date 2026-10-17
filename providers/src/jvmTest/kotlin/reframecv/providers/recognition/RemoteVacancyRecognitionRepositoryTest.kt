package reframecv.providers.recognition

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.recognition.RecognitionDocument
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionOutcome

class RemoteVacancyRecognitionRepositoryTest {
    private val document = RecognitionDocument("job.pdf", "%PDF-1.7".encodeToByteArray())

    @Test
    fun sendsDocumentAndPackagedSchemaAndMapsGeneratedJsonToDomainFields() = runBlocking {
        val schema = vacancyRecognitionSchema()
        assertTrue(schema.contains("urn:reframecv:vacancy-recognition:1"))
        val repository = RemoteVacancyRecognitionRepository(
            provider { request ->
                assertEquals(schema, request.responseSchema)
                assertTrue(request.instruction.contains("Do not invent missing facts"))
                val file = request.documents.single()
                assertEquals("job.pdf", file.fileName)
                assertEquals("application/pdf", file.mediaType)
                assertContentEquals(document.bytes, file.bytes)
                AiResponse.Generated(recognitionFixture())
            },
            schema,
        )
        val result = assertIs<RecognitionOutcome.Recognized>(repository.recognize(document)).result
        assertEquals("Mobile developer", result.jobTitle)
        assertEquals(listOf("Kotlin", "Android"), result.keywords)
        assertEquals("Recognized company", result.company)
    }

    @Test
    fun providerFailuresBecomeRecognitionFailuresWithoutLosingDetails() = runBlocking {
        val errors = mapOf(
            AiProviderError.NotConfigured to RecognitionError.NotConfigured,
            AiProviderError.Unavailable to RecognitionError.ServiceUnavailable,
            AiProviderError.InvalidResponse to RecognitionError.InvalidResponse,
        )
        errors.forEach { (input, expected) ->
            val repository = RemoteVacancyRecognitionRepository(
                provider { AiResponse.Failed(input, "Provider details") },
                "{}",
            )
            assertEquals(
                RecognitionOutcome.Failed(expected, "Provider details"),
                repository.recognize(document),
            )
        }
    }

    @Test
    fun malformedOrIncompleteGeneratedJsonIsAnInvalidResponse() = runBlocking {
        listOf("not JSON", "{}", "[]").forEach { content ->
            val repository = RemoteVacancyRecognitionRepository(
                provider { AiResponse.Generated(content) },
                "{}",
            )
            assertEquals(
                RecognitionOutcome.Failed(RecognitionError.InvalidResponse),
                repository.recognize(document),
            )
        }
    }

    @Test
    fun cancellationIsPropagatedInsteadOfBecomingAProviderError() = runBlocking {
        val failure = CancellationException("Cancelled")
        val repository = RemoteVacancyRecognitionRepository(provider { throw failure }, "{}")
        assertSame(
            failure,
            assertFailsWith<CancellationException> {
                repository.recognize(document)
            },
        )
    }

    private fun provider(action: suspend (AiRequest) -> AiResponse) = object : AiProviderClient {
        override suspend fun execute(request: AiRequest) = action(request)
    }
}
