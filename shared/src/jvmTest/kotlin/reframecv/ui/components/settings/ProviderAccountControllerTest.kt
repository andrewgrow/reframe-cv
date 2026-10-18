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
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ProviderSettingsUseCases

class ProviderAccountControllerTest : ComponentTest() {
    private var clock = 1_000_000L
    private val ticks = Channel<Unit>(Channel.UNLIMITED)
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
                override suspend fun save(connection: ProviderConnection) = connection.id
                override suspend fun activate(id: Long) = Unit
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
                awaitNextRefresh = { _ -> ticks.receive() },
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
