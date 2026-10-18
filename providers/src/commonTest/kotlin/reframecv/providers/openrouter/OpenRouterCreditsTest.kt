package reframecv.providers.openrouter

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.network.HttpTrafficLogger
import reframecv.network.createNetworkClient
import reframecv.providers.RemoteProviderAccountRepository
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

class OpenRouterCreditsTest {
    @Test
    fun usesManagementKeyOnlyForCreditsAndRedactsItFromTraffic() = runBlocking<Unit> {
        val paths = mutableListOf<String>()
        val logs = mutableListOf<String>()
        val client = createNetworkClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                paths += path
                assertEquals(
                    if (path.endsWith(
                            "credits",
                        )
                    ) {
                        "Bearer management-secret"
                    } else {
                        "Bearer ordinary-secret"
                    },
                    request.headers["Authorization"],
                )
                respond(
                    when {
                        path.endsWith(
                            "credits",
                        ) -> """
                            {"data":{"total_credits":100.5,"total_usage":25.75},
                             "echo":"management-secret"}
                        """.trimIndent()

                        path.endsWith("key") -> """{"data":{"limit":50,"limit_remaining":49}}"""

                        else -> """{"data":[]}"""
                    },
                )
            },
        )
        try {
            val setup = OpenRouterSetupClient(client, HttpTrafficLogger { logs += it })
            val repository =
                RemoteProviderAccountRepository(mapOf(ApiProviderType.OpenRouter to setup))
            val balance = repository.balance(ApiProviderType.OpenRouter, "management-secret")
            assertEquals(74.75, balance.remainingUsd)
            assertTrue(balance.fetchedAt > 0)
            assertEquals(
                49.0,
                repository.load(
                    ApiProviderType.OpenRouter,
                    "ordinary-secret",
                ).budgets.single().remainingUsd,
            )
            setup.loadModels("ordinary-secret")
            assertEquals(
                listOf("/api/v1/credits", "/api/v1/key", "/api/v1/key", "/api/v1/models/user"),
                paths,
            )
            assertFalse(logs.joinToString().contains("management-secret"))
            assertFalse(logs.joinToString().contains("ordinary-secret"))
        } finally {
            client.close()
            client.engine.close()
        }
    }

    @Test
    fun rejectedManagementKeysAndMalformedBalancesHaveSafeErrorsAndDoNotBreakBudgets() =
        runBlocking<Unit> {
            var body = "management-secret"
            var status = HttpStatusCode.Forbidden
            val client = createNetworkClient(
                MockEngine { request ->
                    if (request.url.encodedPath.endsWith(
                            "key",
                        )
                    ) {
                        respond("""{"data":{}}""")
                    } else {
                        respond(body, status)
                    }
                },
            )
            try {
                val setup = OpenRouterSetupClient(client)
                val rejected =
                    assertFailsWith<ProviderSetupException> { setup.balance("management-secret") }
                assertEquals(ProviderSetupError.Unauthorized, rejected.reason)
                assertFalse(rejected.toString().contains("management-secret"))
                status = HttpStatusCode.OK
                listOf(
                    "{}",
                    """{"data":{"total_credits":50}}""",
                    """{"data":{"total_credits":50,"total_usage":-1}}""",
                ).forEach {
                    body = it
                    assertEquals(
                        ProviderSetupError.InvalidResponse,
                        assertFailsWith<ProviderSetupException> {
                            setup.balance("management-secret")
                        }.reason,
                    )
                }
                assertTrue(setup.accountInfo("ordinary-secret").budgets.isEmpty())
                assertFailsWith<ProviderSetupException> {
                    RemoteProviderAccountRepository(
                        emptyMap(),
                    ).balance(ApiProviderType.OpenRouter, "management-secret")
                }
            } finally {
                client.close()
                client.engine.close()
            }
        }

    @Test
    fun creditRequestCancellationPropagates() = runBlocking<Unit> {
        val client = createNetworkClient(MockEngine { throw CancellationException("Stopped") })
        try {
            assertFailsWith<CancellationException> {
                OpenRouterSetupClient(client).balance("management-secret")
            }
        } finally {
            client.close()
            client.engine.close()
        }
    }
}
