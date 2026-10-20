package reframecv.ui.components.common.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.ProviderCreditBalance
import reframecv.testing.ComponentTest

class AiRequestBalanceTest : ComponentTest() {
    private val connection = ProviderConnection(
        type = ApiProviderType.OpenRouter,
        name = "Work",
        apiKey = "model-key",
        modelId = "model",
        managementApiKey = "management-key",
    )

    @Test
    fun refreshesTheUsedConnectionCachesBalanceAndRestartsNoticeExpiry() = runBlocking {
        val id = dependencies.providerSettingsUseCases.save(connection)
        dependencies.providerSettingsUseCases.save(connection.copy(managementApiKey = "other"))
        var expiry = CompletableDeferred<Unit>()
        val balance = controller { expiry.await() }
        try {
            balance.onRequestFinished(id)
            assertEquals(listOf("management-key"), dependencies.balanceRequests)
            assertEquals(99.99, balance.state.value.balance!!.remainingUsd)
            assertEquals(
                99.99,
                dependencies.providerSettingsUseCases.observe().first()
                    .single { it.id == id }.creditBalance!!.remainingUsd,
            )
            val oldExpiry = expiry
            expiry = CompletableDeferred()
            dependencies.balanceResponse = { ProviderCreditBalance(100.0, 1.0, 2) }
            balance.onRequestFinished(id)
            oldExpiry.complete(Unit)
            assertEquals(99.0, balance.state.value.balance!!.remainingUsd)
            expiry.complete(Unit)
            assertNull(balance.state.value.balance)
        } finally {
            balance.close()
        }
    }

    @Test
    fun skipsMissingKeysDisabledAndDeletedConnectionsAndContainsRefreshFailures() = runBlocking {
        val noKey = dependencies.providerSettingsUseCases.save(
            connection.copy(managementApiKey = ""),
        )
        val disabled = dependencies.providerSettingsUseCases.save(connection.copy(active = false))
        val id = dependencies.providerSettingsUseCases.save(connection)
        val balance = controller { CompletableDeferred<Unit>().await() }
        try {
            balance.onRequestFinished(noKey)
            balance.onRequestFinished(disabled)
            balance.onRequestFinished(999)
            assertEquals(emptyList(), dependencies.balanceRequests)
            dependencies.balanceResponse = { error("API key expired") }
            balance.onRequestFinished(id)
            assertNull(balance.state.value.balance)
            assertEquals(
                "Refresh balance after AI request",
                dependencies.logger.failures.receive().operation,
            )
        } finally {
            balance.close()
        }
    }

    @Test
    fun changedCredentialsAndClosingTheApplicationDiscardLateBalances() = runBlocking {
        val id = dependencies.providerSettingsUseCases.save(connection)
        val response = CompletableDeferred<ProviderCreditBalance>()
        dependencies.balanceResponse = { response.await() }
        val balance = controller { CompletableDeferred<Unit>().await() }
        balance.onRequestFinished(id)
        dependencies.providerSettingsUseCases.save(
            connection.copy(id = id, managementApiKey = "new"),
        )
        response.complete(ProviderCreditBalance(100.0, 1.0, 2))
        assertNull(balance.state.value.balance)
        assertNull(dependencies.providerSettingsUseCases.observe().first().single().creditBalance)
        val pending = CompletableDeferred<ProviderCreditBalance>()
        dependencies.balanceResponse = { pending.await() }
        balance.onRequestFinished(id)
        balance.close()
        pending.complete(ProviderCreditBalance(100.0, 2.0, 3))
        assertNull(balance.state.value.balance)
    }

    private fun controller(expire: suspend () -> Unit) = AiRequestBalance(
        dependencies.providerSettingsUseCases,
        dependencies.logger,
        CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        expire,
    )
}
