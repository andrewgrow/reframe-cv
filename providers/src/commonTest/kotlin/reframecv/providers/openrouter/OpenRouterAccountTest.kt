package reframecv.providers.openrouter

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.network.createNetworkClient
import reframecv.providers.RemoteProviderAccountRepository
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

class OpenRouterAccountTest {
    @Test
    fun fetchesCurrentBudgetsExpiryAndEffectiveScopeWithoutRequestingModels() = runBlocking<Unit> {
        var requests = 0
        val client = createNetworkClient(
            MockEngine { request ->
                requests++
                assertEquals("/api/v1/key", request.url.encodedPath)
                assertEquals("Bearer example-key", request.headers["Authorization"])
                respond(
                    """
                {"data":{
                  "budgets":[
                    {"scope":"workspace","limit_usd":1,"spend_usd":0.125,"remaining_usd":0.875,
                     "resets_at":"2026-10-19T00:00:00Z","reset_interval":"daily"},
                    {"scope":"key_limit","limit_usd":50,"spend_usd":0.125,"remaining_usd":49.875}
                  ],
                  "effective_budget":{"scope":"workspace","limit_usd":1,"spend_usd":0.125,
                    "remaining_usd":0.875,"resets_at":"2026-10-19T00:00:00Z","reset_interval":"daily"},
                  "expires_at":"2026-10-25T09:38:00.002Z", "limit":999,
                  "creator_user_id":"unused","new_field":{"future":true}
                }}
                    """.trimIndent(),
                )
            },
        )
        try {
            val repository =
                RemoteProviderAccountRepository(
                    mapOf(ApiProviderType.OpenRouter to OpenRouterSetupClient(client)),
                )
            val info = repository.load(ApiProviderType.OpenRouter, "example-key")
            assertEquals(1, requests)
            assertEquals(2, info.budgets.size)
            assertEquals(info.budgets.first(), info.effectiveBudget)
            assertEquals(1.0, info.effectiveBudget?.limitUsd)
            assertEquals(0.875, info.effectiveBudget?.remainingUsd)
            assertEquals(50.0, info.budgets.last().limitUsd)
            assertEquals(
                Instant.parse("2026-10-25T09:38:00.002Z").toEpochMilliseconds(),
                info.expiresAt,
            )
            assertTrue(info.fetchedAt > 0)
        } finally {
            client.close()
            client.engine.close()
        }
    }

    @Test
    fun olderResponsesUseTheCorrectUsagePeriodAndAllowUnknownExpiryAndReset() = runBlocking<Unit> {
        val client = createNetworkClient(
            MockEngine {
                respond(
                    """
                {"data":{"limit":50,"limit_remaining":48,"limit_reset":"monthly",
                "usage":123,"usage_monthly":2}}
                    """.trimIndent(),
                )
            },
        )
        try {
            val info = OpenRouterSetupClient(client).accountInfo("example-key")
            val budget = info.budgets.single()
            assertEquals("key_limit", budget.scope)
            assertEquals(2.0, budget.spendUsd)
            assertEquals(48.0, budget.remainingUsd)
            assertNull(budget.resetsAt)
            assertNull(info.expiresAt)
            assertNull(info.effectiveBudget)
        } finally {
            client.close()
            client.engine.close()
        }
    }

    @Test
    fun malformedMetadataAndMissingProvidersHaveSafeErrors() = runBlocking<Unit> {
        val client =
            createNetworkClient(MockEngine { respond("""{"data":{"budgets":"not-an-array"}}""") })
        try {
            val error = assertFailsWith<ProviderSetupException> {
                OpenRouterSetupClient(client).accountInfo("example-key")
            }
            assertEquals(ProviderSetupError.InvalidResponse, error.reason)
            assertNull(error.cause)
            assertFailsWith<ProviderSetupException> {
                RemoteProviderAccountRepository(emptyMap()).load(ApiProviderType.OpenRouter, "key")
            }
        } finally {
            client.close()
            client.engine.close()
        }
    }
}
