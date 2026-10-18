package reframecv.providers.openrouter

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.network.HttpTrafficLogger
import reframecv.network.createNetworkClient
import reframecv.providers.AiDocument
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse

class OpenRouterClientTest {
    private val connection = ProviderConnection(
        id = 1,
        type = ApiProviderType.OpenRouter,
        name = "Test",
        apiKey = "test-secret",
        modelId = "test-model",
        managementApiKey = "unused-management-secret",
    )
    private fun request(connection: ProviderConnection = this.connection) = AiRequest(
        "Extract in English",
        """{"type":"object","properties":{},"additionalProperties":false}""",
        listOf(AiDocument("job.pdf", "application/pdf", "%PDF".encodeToByteArray())),
        connection,
    )

    @Test
    fun sendsSelectedModelPromptPdfAndStrictSchemaAndLogsWithoutCredentialsOrFileBytes() =
        runBlocking {
            val logs = mutableListOf<String>()
            val client = OpenRouterClient(
                createNetworkClient(
                    MockEngine { http ->
                        assertEquals("Bearer test-secret", http.headers[HttpHeaders.Authorization])
                        assertEquals(
                            "https://openrouter.ai/api/v1/chat/completions",
                            http.url.toString(),
                        )
                        assertRequestBody((http.body as TextContent).text)
                        respond(
                            """{"choices":[{"finish_reason":"stop",
                    "message":{"content":"{\"jobTitle\":\"Developer\"}"}}]}""",
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ),
                HttpTrafficLogger { logs += it },
            )
            try {
                assertEquals(
                    AiResponse.Generated("""{"jobTitle":"Developer"}"""),
                    client.execute(request()),
                )
                assertTrue(logs.any { it.contains("REQUEST POST") })
                assertTrue(logs.any { it.contains("RESPONSE 200") })
                assertTrue(
                    logs.none {
                        it.contains("test-secret") || it.contains("JVBERg==") ||
                            it.contains("unused-management-secret")
                    },
                )
            } finally {
                client.close()
            }
        }

    private fun assertRequestBody(content: String) {
        val body = Json.parseToJsonElement(
            content,
        ).jsonObject
        assertEquals("test-model", body.getValue("model").jsonPrimitive.content)
        assertEquals(
            true,
            body.getValue(
                "provider",
            ).jsonObject.getValue("require_parameters").jsonPrimitive.boolean,
        )
        val schema = body.getValue(
            "response_format",
        ).jsonObject.getValue("json_schema").jsonObject
        assertEquals(true, schema.getValue("strict").jsonPrimitive.boolean)
        assertEquals(
            Json.parseToJsonElement(request().responseSchema),
            schema["schema"],
        )
        val parts = body.getValue(
            "messages",
        ).jsonArray.single().jsonObject.getValue("content").jsonArray
        assertEquals(
            "Extract in English",
            parts[0].jsonObject.getValue("text").jsonPrimitive.content,
        )
        val file = parts[1].jsonObject.getValue("file").jsonObject
        assertEquals("job.pdf", file.getValue("filename").jsonPrimitive.content)
        assertEquals(
            "data:application/pdf;base64,JVBERg==",
            file.getValue("file_data").jsonPrimitive.content,
        )
    }

    @Test
    fun missingCredentialsModelOrDisabledConnectionNeverSendRequests() = runBlocking {
        var requests = 0
        val client =
            OpenRouterClient(
                createNetworkClient(
                    MockEngine {
                        requests++
                        error("No request expected")
                    },
                ),
            )
        try {
            listOf(
                connection.copy(apiKey = ""),
                connection.copy(modelId = ""),
                connection.copy(active = false),
            ).forEach {
                assertEquals(
                    AiResponse.Failed(AiProviderError.NotConfigured),
                    client.execute(request(it)),
                )
            }
            assertEquals(0, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun providerErrorsAreReturnedWithSafeDetailsIncludingHttp200ErrorEnvelopes() = runBlocking {
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.OK)) {
            val client = OpenRouterClient(
                createNetworkClient(
                    MockEngine {
                        respond("""{"error":{"message":"Rejected test-secret"}}""", status)
                    },
                ),
            )
            try {
                assertEquals(
                    AiResponse.Failed(AiProviderError.Unavailable, "Rejected [REDACTED]"),
                    client.execute(request()),
                )
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun malformedEmptyTruncatedOrRefusedResponsesAreRejected() = runBlocking {
        val bodies = listOf(
            "not json",
            "{}",
            """{"choices":[]}""",
            """{"choices":[{"message":{"content":null}}]}""",
            """{"choices":[{"finish_reason":"length","message":{"content":"{}"}}]}""",
            """{"choices":[{"message":{"content":"{}","refusal":"No"}}]}""",
        )
        bodies.forEach { body ->
            val client = OpenRouterClient(createNetworkClient(MockEngine { respond(body) }))
            try {
                assertEquals(
                    AiResponse.Failed(AiProviderError.InvalidResponse),
                    client.execute(request()),
                    body,
                )
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun transportFailuresAreSafeAndCancellationIsPropagated() = runBlocking {
        val client =
            OpenRouterClient(createNetworkClient(MockEngine { error("Unsafe test-secret") }))
        try {
            assertEquals(AiResponse.Failed(AiProviderError.Unavailable), client.execute(request()))
        } finally {
            client.close()
        }
        val cancelled =
            OpenRouterClient(
                createNetworkClient(
                    MockEngine {
                        throw CancellationException("Cancelled")
                    },
                ),
            )
        try {
            assertFailsWith<CancellationException> { cancelled.execute(request()) }
        } finally {
            cancelled.close()
        }
        Unit
    }
}
