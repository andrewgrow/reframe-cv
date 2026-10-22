package reframecv.providers.improvement

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeExperience
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeProfile
import reframecv.improvement.ResumeImprovementOutcome
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.serialization.ResumeContentJson

class RemoteResumeImprovementTest {
    private val connection =
        ProviderConnection(
            type = ApiProviderType.OpenRouter,
            name = "Work",
            configuration = OpenRouterConfiguration(
                apiKey = "key",
                modelId = "model",
            ),
        )
    private val content = ResumeContent(
        profile = ResumeProfile(name = "Alex", workArrangement = "Hybrid"),
        sections = listOf(
            ResumeExperienceSection(entries = listOf(ResumeExperience(description = "Context"))),
        ),
    )

    @Test
    fun sendsNamedDocumentsAndAcceptsTheExtendedResumeContract() = runBlocking {
        val schemaText = resumeImprovementSchema()
        val encoded = ResumeContentJson.encodeForExport(content)
        val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
            it.schemas(mapOf("urn:improvement:test" to schemaText))
        }
        val schema = registry.getSchema(SchemaLocation.of("urn:improvement:test"))
        assertTrue(schema.validate(encoded, InputFormat.JSON).isEmpty())
        assertTrue(
            schema.validate("""{"formatVersion":1,"content":{}}""", InputFormat.JSON).isNotEmpty(),
        )
        val requests = mutableListOf<AiRequest>()
        val repository = RemoteResumeImprovementRepository(
            object : AiProviderClient {
                override suspend fun execute(request: AiRequest): AiResponse {
                    requests += request
                    return AiResponse.Generated(encoded)
                }
            },
            schemaText,
        )
        val result = repository.improve("source", "vacancy", connection, "Improve")
        assertEquals(content, assertIs<ResumeImprovementOutcome.Improved>(result).content)
        val request = requests.single()
        assertEquals(listOf("resume.json", "vacancy.json"), request.textDocuments.map { it.name })
        assertEquals(listOf("source", "vacancy"), request.textDocuments.map { it.content })
        assertTrue(request.documents.isEmpty())
        assertEquals("Improve", request.instruction)
        assertEquals(schemaText, request.responseSchema)
    }

    @Test
    fun malformedResponsesAndProviderFailuresStayErrorsAndCancellationPropagates() =
        runBlocking<Unit> {
            var response: AiResponse = AiResponse.Failed(
                AiProviderError.Unavailable,
                "API key expired.",
            )
            var cancelled = false
            val repository = RemoteResumeImprovementRepository(
                object : AiProviderClient {
                    override suspend fun execute(request: AiRequest): AiResponse {
                        if (cancelled) throw CancellationException("Closed")
                        return response
                    }
                },
                resumeImprovementSchema(),
            )
            assertEquals(
                ResumeImprovementOutcome.Failed("API key expired."),
                repository.improve("{}", "{}", connection, "Improve"),
            )
            listOf(
                "not JSON",
                "[]",
                "{}",
                """{"formatVersion":"1","content":{}}""",
                """{"formatVersion":1,"content":{"sections":[{"type":"summary","visible":"true"}]}}""",
                """{"formatVersion":2,"content":{}}""",
                """{"formatVersion":1,"content":{"unknown":true}}""",
                """{"formatVersion":1,"content":{"profile":{"name":42}}}""",
                """{"formatVersion":1,"content":{"sections":[{"type":"future"}]}}""",
                """{"formatVersion":1,"content":{"consentStatement":"legacy"}}""",
            ).forEach { invalid ->
                response = AiResponse.Generated(invalid)
                assertIs<ResumeImprovementOutcome.Failed>(
                    repository.improve("{}", "{}", connection, "Improve"),
                )
            }
            response = AiResponse.Failed(AiProviderError.Unavailable)
            assertIs<ResumeImprovementOutcome.Failed>(
                repository.improve("{}", "{}", connection, "Improve"),
            )
            cancelled = true
            assertFailsWith<CancellationException> {
                repository.improve("{}", "{}", connection, "Improve")
            }
        }
}
