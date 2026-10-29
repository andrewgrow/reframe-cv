package reframecv.providers.openai

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import reframecv.network.HttpTrafficLogger
import reframecv.network.createNetworkClient
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

class OpenAiSetupClientTest {
    @Test
    fun loadsAccessibleIdsWithoutInventingCapabilitiesAndLogsSafely() = runBlocking {
        val logs = mutableListOf<String>()
        val http = createNetworkClient(
            MockEngine { request ->
                assertEquals("https://api.openai.com/v1/models", request.url.toString())
                assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
                respond(
                    """{"data":[{"id":"z-model","object":"model"},{"id":"a-model"},
                {"id":"a-model"},{"id":""}]}""",
                )
            },
        )
        try {
            val models = OpenAiSetupClient(http, HttpTrafficLogger { logs += it })
                .loadModels(" test-key ")
            assertEquals(listOf("a-model", "z-model"), models.map { it.id })
            assertTrue(models.all { it.id == it.name && !it.structuredOutput })
            assertEquals(2, logs.size)
            assertFalse(logs.any { "test-key" in it })
        } finally {
            http.close()
        }
    }

    @Test
    fun mapsAuthenticationAndOtherErrorsAndKeepsProviderFeedbackSafe() = runBlocking {
        for (status in listOf(401, 403, 429, 500)) {
            val http = createNetworkClient(
                MockEngine {
                    respond(
                        """{"error":{"message":"Invalid test-key"}}""",
                        HttpStatusCode.fromValue(status),
                    )
                },
            )
            try {
                val error = assertFailsWith<ProviderSetupException> {
                    OpenAiSetupClient(http).loadModels("test-key")
                }
                assertEquals(
                    if (status in listOf(401, 403)) {
                        ProviderSetupError.Unauthorized
                    } else {
                        ProviderSetupError.Unavailable
                    },
                    error.reason,
                )
                assertEquals("Invalid [REDACTED]", error.providerMessage)
            } finally {
                http.close()
            }
        }
    }

    @Test
    fun rejectsMalformedCatalogAndPropagatesCancellation() = runBlocking {
        for (content in listOf("bad", "{}", """{"data":[{}]}""")) {
            val http = createNetworkClient(MockEngine { respond(content) })
            try {
                assertEquals(
                    ProviderSetupError.InvalidResponse,
                    assertFailsWith<ProviderSetupException> {
                        OpenAiSetupClient(http).loadModels("key")
                    }.reason,
                )
            } finally {
                http.close()
            }
        }
        val http = createNetworkClient(MockEngine { throw CancellationException("Closed") })
        try {
            assertFailsWith<CancellationException> { OpenAiSetupClient(http).loadModels("key") }
        } finally {
            http.close()
        }
        Unit
    }
}
