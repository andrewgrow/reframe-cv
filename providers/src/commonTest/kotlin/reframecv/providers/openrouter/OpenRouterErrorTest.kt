package reframecv.providers.openrouter

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import reframecv.network.createNetworkClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiResponse
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

class OpenRouterErrorTest {
    @Test
    fun expiredKeysExposeRedactedProviderMessageForEverySetupOperation() = runBlocking {
        val client = createNetworkClient(
            MockEngine {
                respond(
                    """{"error":{"message":"API key expired: test-secret.","code":401}}""",
                    HttpStatusCode.Unauthorized,
                )
            },
        )
        try {
            val setup = OpenRouterSetupClient(client)
            val operations: List<suspend () -> Unit> = listOf(
                { setup.balance("test-secret") },
                { setup.accountInfo("test-secret") },
                { setup.loadModels("test-secret") },
            )
            for (operation in operations) {
                val failure = assertFailsWith<ProviderSetupException> { operation() }
                assertEquals(ProviderSetupError.Unauthorized, failure.reason)
                assertEquals("API key expired: [REDACTED].", failure.providerMessage)
                assertNull(failure.cause)
            }
        } finally {
            client.close()
            client.engine.close()
        }
    }

    @Test
    fun missingMalformedAndNonTextMessagesUseFallbacks() {
        for (body in listOf("<html>Bad gateway</html>", "[]", "{}", """{"error":"bad"}""")) {
            assertNull(openRouterErrorMessage(body, "secret"))
        }
        for (value in listOf("null", "false", "401", "[]", "{}", "\"  \"")) {
            assertNull(openRouterErrorMessage("""{"error":{"message":$value}}""", "secret"))
        }
        assertEquals(
            AiResponse.Failed(AiProviderError.Unavailable, "HTTP 502"),
            decodeGeneration("<html>Bad gateway</html>", 502, "secret"),
        )
    }

    @Test
    fun generationErrorsUseTheSameMessageForHttpAndSuccessfulEnvelopes() {
        val body = """{"error":{"message":"  Rate limit exceeded for secret.  "}}"""
        for (status in listOf(200, 429)) {
            assertEquals(
                AiResponse.Failed(
                    AiProviderError.Unavailable,
                    "Rate limit exceeded for [REDACTED].",
                ),
                decodeGeneration(body, status, "secret"),
            )
        }
    }
}
