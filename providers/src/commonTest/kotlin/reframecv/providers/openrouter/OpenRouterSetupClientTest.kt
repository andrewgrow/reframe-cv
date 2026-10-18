package reframecv.providers.openrouter

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import reframecv.network.HttpTrafficLogger
import reframecv.network.createNetworkClient
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

class OpenRouterSetupClientTest {
    @Test
    fun checksTheKeyBeforeLoadingUserModelsAndPreservesCapabilities() = runBlocking<Unit> {
        val paths = mutableListOf<String>()
        val client = createNetworkClient(
            MockEngine { request ->
                assertEquals("Bearer example-key", request.headers["Authorization"])
                paths += request.url.encodedPath
                respond(
                    if (request.url.encodedPath.endsWith("/key")) {
                        """{"data":{}}"""
                    } else {
                        """{"data":[{"id":"z","name":"Zulu","supported_parameters":["structured_outputs"]},{"id":"a","name":"Alpha"},{"id":"z","name":"Duplicate"}],"extra":1}"""
                    },
                )
            },
        )
        try {
            val models = OpenRouterSetupClient(
                client,
            ).loadModels("example-key")
            assertEquals(listOf("/api/v1/key", "/api/v1/models/user"), paths)
            assertEquals(listOf("a", "z"), models.map { it.id })
            assertFalse(models.first().structuredOutput)
            assertTrue(models.last().structuredOutput)
        } finally {
            client.close()
            client.engine.close()
        }
    }

    @Test
    fun rejectedCredentialsStopBeforeModelDiscoveryAndNeverExposeResponseBodies() =
        runBlocking<Unit> {
            val traffic = mutableListOf<String>()
            var requests = 0
            val client = createNetworkClient(
                MockEngine {
                    requests++
                    respond("example-key should not escape", HttpStatusCode.Unauthorized)
                },
            )
            try {
                val failure = assertFailsWith<ProviderSetupException> {
                    OpenRouterSetupClient(
                        client,
                        HttpTrafficLogger { traffic += it },
                    ).loadModels("example-key")
                }
                assertEquals(ProviderSetupError.Unauthorized, failure.reason)
                assertEquals(1, requests)
                assertTrue(traffic.any { it.contains("RESPONSE 401") })
                assertFalse(traffic.joinToString().contains("example-key"))
                assertFalse(failure.toString().contains("example-key"))
                assertNull(failure.cause)
            } finally {
                client.close()
                client.engine.close()
            }
        }

    @Test
    fun malformedCatalogIsReportedAsInvalidResponse() = runBlocking<Unit> {
        val client = createNetworkClient(
            MockEngine { request ->
                respond(if (request.url.encodedPath.endsWith("/key")) """{"data":{}}""" else "{}")
            },
        )
        try {
            val failure = assertFailsWith<ProviderSetupException> {
                OpenRouterSetupClient(client).loadModels("example-key")
            }
            assertEquals(ProviderSetupError.InvalidResponse, failure.reason)
        } finally {
            client.close()
            client.engine.close()
        }
    }

    @Test
    fun cancellationRemainsCancellation() = runBlocking<Unit> {
        val client = createNetworkClient(MockEngine { throw CancellationException("Stopped") })
        try {
            assertFailsWith<CancellationException> {
                OpenRouterSetupClient(client).loadModels("example-key")
            }
        } finally {
            client.close()
            client.engine.close()
        }
    }
}
