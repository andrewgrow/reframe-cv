package reframecv.ui.components.settings

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderBudget
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.ProviderCreditBalance
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ProviderSettingsUseCases

class ProviderAccountControllerTest : ComponentTest() {
    private var clock = 1_000_000L
    private val ticks = Channel<Unit>(Channel.UNLIMITED)
    private val waiting = Channel<Unit>(Channel.UNLIMITED)
    private val writes = mutableListOf<ProviderAccountInfo>()
    private val cached = ProviderAccountInfo(
        listOf(ProviderBudget("key_limit", 50.0, 1.0, 49.0)),
        fetchedAt = clock,
    )
    private val state = MutableValue(
        ProviderEditorState(
            ProviderConnection(
                id = 7,
                type = ApiProviderType.OpenRouter,
                name = "Work",
                apiKey = "old",
                modelId = "model",
                accountInfo = cached,
            ),
        ),
    )

    private fun controller(accounts: ProviderAccountRepository): ProviderAccountController =
        runOnUiThread {
            lifecycle.resume()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            lifecycle.doOnDestroy { scope.cancel() }
            val connections = object : ProviderConnectionsRepository {
                override fun observe() = MutableStateFlow(emptyList<ProviderConnection>())
                override suspend fun get(id: Long) = state.value.connection.takeIf { it.id == id }
                override suspend fun save(connection: ProviderConnection) = connection.id
                override suspend fun setActive(id: Long, active: Boolean) = Unit
                override suspend fun updateCreditBalance(
                    id: Long,
                    type: ApiProviderType,
                    managementApiKey: String,
                    balance: reframecv.domain.models.providers.ProviderCreditBalance,
                ) = Unit
                override suspend fun updateAccountInfo(
                    id: Long,
                    type: ApiProviderType,
                    apiKey: String,
                    info: ProviderAccountInfo,
                ) {
                    writes += info
                }
            }
            ProviderAccountController(
                ProviderSettingsUseCases(
                    connections,
                    ProviderModelsRepository { _, _ ->
                        emptyList()
                    },
                    accounts,
                ),
                lifecycle,
                scope,
                state,
                now = { clock },
                awaitNextRefresh = { _ ->
                    waiting.send(Unit)
                    ticks.receive()
                },
            )
        }

    @Test
    fun refreshesStaleDataOnTicksAndResumeButNeverWhileThePageIsPaused() = runBlocking {
        var requests = 0
        controller(
            ProviderAccountRepository { _, _ ->
                requests++
                cached.copy(fetchedAt = clock)
            },
        )
        runOnUiThread { assertEquals(0, requests) }
        runOnUiThread { clock += ACCOUNT_REFRESH_INTERVAL_MILLIS }
        ticks.send(Unit)
        await { it.connection.accountInfo?.fetchedAt == clock }
        runOnUiThread {
            assertEquals(1, requests)
            lifecycle.pause()
            clock += ACCOUNT_REFRESH_INTERVAL_MILLIS
        }
        ticks.send(Unit)
        runOnUiThread {
            assertEquals(1, requests)
            lifecycle.resume()
        }
        await { it.connection.accountInfo?.fetchedAt == clock }
        runOnUiThread {
            assertEquals(2, requests)
            assertEquals(2, writes.size)
        }
    }

    @Test
    fun disabledConnectionsSkipTimerAndManualRefreshUntilEnabled() = runBlocking {
        var requests = 0
        val controller = controller(
            ProviderAccountRepository { _, _ ->
                requests++
                cached.copy(fetchedAt = clock)
            },
        )
        withTimeout(10_000) { waiting.receive() }
        runOnUiThread {
            state.value = state.value.copy(connection = state.value.connection.copy(active = false))
            clock += ACCOUNT_REFRESH_INTERVAL_MILLIS
            controller.refresh()
        }
        ticks.send(Unit)
        withTimeout(10_000) { waiting.receive() }
        runOnUiThread {
            assertEquals(0, requests)
            assertTrue(writes.isEmpty())
            state.value = state.value.copy(connection = state.value.connection.copy(active = true))
        }
        ticks.send(Unit)
        await { it.connection.accountInfo?.fetchedAt == clock }
        runOnUiThread { assertEquals(1, requests) }
    }

    @Test
    fun failuresKeepTheCachedValuesAndManualRetryPreservesEditsMadeDuringTheRequest() =
        runBlocking {
            var fail = true
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val controller = controller(
                ProviderAccountRepository { _, _ ->
                    if (fail) error("Network unavailable")
                    started.complete(Unit)
                    release.await()
                    cached.copy(fetchedAt = clock + 1)
                },
            )
            runOnUiThread { controller.refresh() }
            await { it.accountError != null }
            runOnUiThread {
                assertEquals(cached, state.value.connection.accountInfo)
                assertFalse(state.value.refreshingAccount)
                fail = false
                controller.refresh()
            }
            withTimeout(10_000) { started.await() }
            runOnUiThread {
                state.value =
                    state.value.copy(connection = state.value.connection.copy(name = "Edited name"))
            }
            release.complete(Unit)
            await { !it.refreshingAccount }
            runOnUiThread {
                assertEquals("Edited name", state.value.connection.name)
                assertNull(state.value.accountError)
                assertEquals(clock + 1, state.value.connection.accountInfo?.fetchedAt)
                assertEquals(1, writes.size)
            }
        }

    @Test
    fun cancellingOldCredentialsDiscardsLateResponsesBeforeTheyCanReachTheDatabase() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        val controller = controller(
            ProviderAccountRepository { _, _ ->
                started.complete(Unit)
                withContext(NonCancellable) { release.await() }
                returned.complete(Unit)
                cached.copy(fetchedAt = clock + 1)
            },
        )
        runOnUiThread { controller.refresh() }
        withTimeout(10_000) { started.await() }
        runOnUiThread {
            controller.cancel()
            state.value =
                state.value.copy(
                    connection = state.value.connection.copy(apiKey = "new", accountInfo = null),
                )
        }
        release.complete(Unit)
        withTimeout(10_000) { returned.await() }
        runOnUiThread {
            assertNull(state.value.connection.accountInfo)
            assertTrue(writes.isEmpty())
            assertFalse(state.value.refreshingAccount)
        }
    }

    @Test
    fun optionalBalanceFailuresNeverBlockBudgetsAndBudgetFailuresNeverBlockBalance() = runBlocking {
        var budgetRequests = 0
        var balanceRequests = 0
        var failBalance = true
        var failBudgets = false
        val controller = controller(object : ProviderAccountRepository {
            override suspend fun load(type: ApiProviderType, apiKey: String): ProviderAccountInfo {
                budgetRequests++
                if (failBudgets) error("Ordinary key rejected")
                return cached.copy(fetchedAt = clock + 1)
            }
            override suspend fun balance(
                type: ApiProviderType,
                managementApiKey: String,
            ): ProviderCreditBalance {
                balanceRequests++
                if (failBalance) error("Management key rejected")
                return ProviderCreditBalance(50.0, 2.0, clock)
            }
        })
        runOnUiThread { controller.refresh() }
        await { it.connection.accountInfo?.fetchedAt == clock + 1 }
        runOnUiThread {
            assertEquals(1, budgetRequests)
            assertEquals(0, balanceRequests)
            state.value =
                state.value.copy(
                    connection = state.value.connection.copy(managementApiKey = "management"),
                )
            controller.refresh()
        }
        await { it.balanceError && !it.refreshingAccount }
        runOnUiThread {
            assertEquals(2, budgetRequests)
            assertNull(state.value.accountError)
            failBalance = false
            failBudgets = true
            controller.refresh()
        }
        await { it.accountError != null && it.connection.creditBalance != null }
        runOnUiThread {
            assertEquals(48.0, state.value.connection.creditBalance?.remainingUsd)
            assertFalse(state.value.balanceError)
            assertEquals(clock + 1, state.value.connection.accountInfo?.fetchedAt)
        }
    }

    @Test
    fun staleBalanceRefreshesWithoutReloadingFreshBudgetsAndStopsWhenDisabled() = runBlocking {
        var balanceRequests = 0
        var budgetRequests = 0
        runOnUiThread {
            state.value =
                state.value.copy(
                    connection = state.value.connection.copy(managementApiKey = "management"),
                )
        }
        val controller = controller(object : ProviderAccountRepository {
            override suspend fun load(type: ApiProviderType, apiKey: String): ProviderAccountInfo {
                budgetRequests++
                return cached.copy(fetchedAt = clock)
            }
            override suspend fun balance(
                type: ApiProviderType,
                managementApiKey: String,
            ): ProviderCreditBalance {
                balanceRequests++
                return ProviderCreditBalance(50.0, 2.0, clock)
            }
        })
        await { it.connection.creditBalance != null }
        withTimeout(10_000) { waiting.receive() }
        runOnUiThread {
            assertEquals(1, balanceRequests)
            assertEquals(0, budgetRequests)
            state.value = state.value.copy(connection = state.value.connection.copy(active = false))
            controller.cancel()
            clock += ACCOUNT_REFRESH_INTERVAL_MILLIS
            controller.refresh()
        }
        ticks.send(Unit)
        withTimeout(10_000) { waiting.receive() }
        runOnUiThread {
            assertEquals(1, balanceRequests)
            assertEquals(0, budgetRequests)
            state.value = state.value.copy(connection = state.value.connection.copy(active = true))
            lifecycle.pause()
            lifecycle.resume()
        }
        await { it.connection.creditBalance?.fetchedAt == clock }
        runOnUiThread { assertEquals(2, balanceRequests) }
    }

    @Test
    fun cancellingBalanceDiscardsLateResponsesForOldCredentials() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        runOnUiThread {
            state.value =
                state.value.copy(
                    connection = state.value.connection.copy(managementApiKey = "old-management"),
                )
        }
        val controller = controller(object : ProviderAccountRepository {
            override suspend fun load(type: ApiProviderType, apiKey: String) = cached
            override suspend fun balance(
                type: ApiProviderType,
                managementApiKey: String,
            ): ProviderCreditBalance {
                started.complete(Unit)
                withContext(NonCancellable) { release.await() }
                returned.complete(Unit)
                return ProviderCreditBalance(50.0, 2.0, clock)
            }
        })
        withTimeout(10_000) { started.await() }
        runOnUiThread {
            controller.cancel()
            state.value =
                state.value.copy(
                    connection = state.value.connection.copy(managementApiKey = "new-management"),
                )
        }
        release.complete(Unit)
        withTimeout(10_000) { returned.await() }
        runOnUiThread {
            assertNull(state.value.connection.creditBalance)
            assertFalse(state.value.refreshingBalance)
            assertFalse(state.value.balanceError)
        }
    }

    private suspend fun await(predicate: (ProviderEditorState) -> Boolean) {
        val ready = CompletableDeferred<Unit>()
        val subscription =
            runOnUiThread { state.subscribe { if (predicate(it)) ready.complete(Unit) } }
        try {
            withTimeout(10_000) { ready.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }
}
