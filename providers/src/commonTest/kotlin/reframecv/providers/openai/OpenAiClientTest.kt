package reframecv.providers.openai

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenAiConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.network.HttpTrafficLogger
import reframecv.network.createNetworkClient
import reframecv.providers.AiDocument
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.providers.AiTextDocument

class OpenAiClientTest {
    private val connection = ProviderConnection(
        id = 1,
        type = ApiProviderType.OpenAI,
        name = "Direct",
        configuration = OpenAiConfiguration(apiKey = "test-secret", modelId = "example-model"),
    )
    private val request = AiRequest(
        "Read the supplied data",
        """{"type":"object","properties":{},"additionalProperties":false}""",
        listOf(
            AiDocument("source.pdf", "application/pdf", "%PDF".encodeToByteArray()),
            AiDocument("source.png", "image/png", "image".encodeToByteArray()),
        ),
        connection,
        listOf(AiTextDocument("resume.json", "{}"), AiTextDocument("vacancy.json", "{}")),
    )
    private val success = """{"status":"completed","output":[{"type":"reasoning","summary":[]},
        {"type":"message","status":"completed","content":[{"type":"output_text","text":"{}"}]}]}"""

    @Test
    fun sendsSelectedModelStrictSchemaJsonPdfAndImageWithoutStoringOrLeakingCredentials() =
        runBlocking {
            val logs = mutableListOf<String>()
            val client = OpenAiClient(
                createNetworkClient(
                    MockEngine { http ->
                        assertEquals("https://api.openai.com/v1/responses", http.url.toString())
                        assertEquals("Bearer test-secret", http.headers[HttpHeaders.Authorization])
                        checkBody((http.body as TextContent).text)
                        respond(success)
                    },
                ),
                HttpTrafficLogger { logs += it },
            )
            try {
                assertEquals(AiResponse.Generated("{}"), client.execute(request))
                assertTrue(logs.any { "REQUEST POST" in it })
                assertTrue(logs.any { "RESPONSE 200" in it })
                listOf("test-secret", "JVBERg==", "aW1hZ2U=").forEach { secret ->
                    assertFalse(logs.any { secret in it }, secret)
                }
            } finally {
                client.close()
            }
        }

    private fun checkBody(content: String) {
        val body = Json.parseToJsonElement(content).jsonObject
        assertEquals("example-model", body.getValue("model").jsonPrimitive.content)
        assertFalse(body.getValue("store").jsonPrimitive.boolean)
        assertFalse(body.getValue("stream").jsonPrimitive.boolean)
        assertFalse("provider" in body)
        val format = body.getValue("text").jsonObject.getValue("format").jsonObject
        assertEquals("json_schema", format.getValue("type").jsonPrimitive.content)
        assertTrue(format.getValue("strict").jsonPrimitive.boolean)
        assertEquals(Json.parseToJsonElement(request.responseSchema), format["schema"])
        val input = body.getValue("input").jsonArray.single().jsonObject
        assertEquals("user", input.getValue("role").jsonPrimitive.content)
        val parts = input.getValue("content").jsonArray.map { it.jsonObject }
        assertEquals(
            listOf("input_text", "input_text", "input_text", "input_file", "input_image"),
            parts.map { it.getValue("type").jsonPrimitive.content },
        )
        assertEquals(request.instruction, parts[0].getValue("text").jsonPrimitive.content)
        assertEquals("resume.json\n{}", parts[1].getValue("text").jsonPrimitive.content)
        assertEquals("vacancy.json\n{}", parts[2].getValue("text").jsonPrimitive.content)
        assertEquals("source.pdf", parts[3].getValue("filename").jsonPrimitive.content)
        assertEquals(
            "data:application/pdf;base64,JVBERg==",
            parts[3]["file_data"]?.jsonPrimitive?.content,
        )
        assertEquals(
            "data:image/png;base64,aW1hZ2U=",
            parts[4]["image_url"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun missingModelKeyDisabledOrDifferentProviderDoesNotReachApi() = runBlocking {
        val client = OpenAiClient(createNetworkClient(MockEngine { error("Must not request") }))
        try {
            val configs = listOf(
                connection.copy(active = false),
                connection.copy(configuration = OpenAiConfiguration(apiKey = "test-secret")),
                connection.copy(configuration = OpenAiConfiguration(modelId = "example-model")),
                connection.copy(type = ApiProviderType.OpenRouter),
                connection.copy(configuration = OpenRouterConfiguration()),
                connection.copy(configuration = OpenAiConfiguration("test-secret", "model", 2)),
            )
            configs.forEach {
                assertEquals(
                    AiResponse.Failed(AiProviderError.NotConfigured),
                    client.execute(request.copy(connection = it)),
                )
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun errorsExposeProviderMessageWithRedactedKey() = runBlocking {
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.OK)) {
            val client = OpenAiClient(
                createNetworkClient(
                    MockEngine {
                        respond("""{"error":{"message":"Expired test-secret"}}""", status)
                    },
                ),
            )
            try {
                assertEquals(
                    AiResponse.Failed(AiProviderError.Unavailable, "Expired [REDACTED]"),
                    client.execute(request),
                )
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun rejectsRefusalsIncompleteEmptyAmbiguousAndMalformedOutput() = runBlocking {
        val invalid = listOf(
            "not JSON", "null", "{}", """{"status":"completed","output":[]}""",
            success.replace("completed", "incomplete"),
            success.replace("output_text", "refusal"),
            success.replace("\"text\":\"{}\"", "\"text\":\"\""),
            success.replace("\"text\":\"{}\"", "\"text\":{}"),
            """{"status":"failed","error":{"code":"server_error"}}""",
        )
        invalid.forEach { content ->
            val client = OpenAiClient(createNetworkClient(MockEngine { respond(content) }))
            try {
                assertEquals(
                    AiResponse.Failed(AiProviderError.InvalidResponse),
                    client.execute(request),
                    content,
                )
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun transportErrorsAreSafeAndCancellationIsPropagated() = runBlocking {
        val client = OpenAiClient(createNetworkClient(MockEngine { error("Unsafe test-secret") }))
        try {
            assertEquals(AiResponse.Failed(AiProviderError.Unavailable), client.execute(request))
        } finally {
            client.close()
        }
        val cancelled = OpenAiClient(
            createNetworkClient(
                MockEngine {
                    throw CancellationException("Closed dialog")
                },
            ),
        )
        try {
            assertFailsWith<CancellationException> { cancelled.execute(request) }
        } finally {
            cancelled.close()
        }
        Unit
    }
}
