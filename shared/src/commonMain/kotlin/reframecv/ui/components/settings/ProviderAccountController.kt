package reframecv.ui.components.settings

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import reframecv.domain.models.providers.ProviderConnection
import reframecv.shared.nowMillis
import reframecv.usecase.ProviderSettingsUseCases

internal const val ACCOUNT_REFRESH_INTERVAL_MILLIS = 5 * 60 * 1000L

/** Refreshes account details only while their editor is visible, preserving drafts and cached data. */
internal class ProviderAccountController(
    private val useCases: ProviderSettingsUseCases,
    private val lifecycle: Lifecycle,
    private val scope: CoroutineScope,
    private val state: MutableValue<ProviderEditorState>,
    private val now: () -> Long = ::nowMillis,
    private val awaitNextRefresh: suspend (Long) -> Unit = { delay(it) },
) {
    private val balance = ProviderBalanceController(useCases, lifecycle, scope, state)
    private var polling: Job? = null
    private var request: Job? = null
    private var generation = 0L
    private var savedConnection = state.value.connection.takeIf { it.id != 0L }

    fun onSaved(connection: ProviderConnection) {
        savedConnection = connection
    }

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onResume() = start()
            override fun onPause() = stop()
            override fun onDestroy() = stop()
        })
        if (lifecycle.state == Lifecycle.State.RESUMED) start()
    }

    private fun start() {
        polling?.cancel()
        polling = scope.launch {
            while (isActive) {
                val draft = state.value
                val updated = draft.connection.accountInfo?.fetchedAt
                val age = updated?.let { now() - it }
                val stale = age == null || age < 0 || age >= ACCOUNT_REFRESH_INTERVAL_MILLIS
                val sameCredentials = savedConnection?.let {
                    it.id == draft.connection.id && it.type == draft.connection.type &&
                        it.apiKey == draft.connection.apiKey.trim()
                } == true
                if (sameCredentials && stale && draft.connection.active) refreshBudgets()
                val balanceWait = balance.refreshIfStale(savedConnection, now())
                val wait = if (stale) {
                    ACCOUNT_REFRESH_INTERVAL_MILLIS
                } else {
                    ACCOUNT_REFRESH_INTERVAL_MILLIS - checkNotNull(age)
                }
                awaitNextRefresh(minOf(wait, balanceWait))
            }
        }
    }

    private fun stop() {
        polling?.cancel()
        polling = null
        cancel()
    }

    fun cancel() {
        balance.cancel()
        generation++
        request?.cancel()
        request = null
        state.value = state.value.copy(refreshingAccount = false)
    }

    fun refreshBalance() = balance.refresh()

    fun refresh() {
        refreshBudgets()
        balance.refresh()
    }

    @Suppress("TooGenericExceptionCaught")
    fun refreshBudgets() {
        val draft = state.value
        val canRefresh =
            lifecycle.state == Lifecycle.State.RESUMED && !draft.busy && draft.connection.active
        if (!canRefresh || draft.refreshingAccount || draft.connection.apiKey.isBlank()) {
            return
        }
        val version = ++generation
        state.value = draft.copy(refreshingAccount = true, accountError = null)
        request = scope.launch {
            try {
                val info = useCases.refreshAccountInfo(draft.connection)
                currentCoroutineContext().ensureActive()
                if (generation == version) {
                    state.value = state.value.copy(
                        connection = state.value.connection.copy(accountInfo = info),
                        refreshingAccount = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (generation == version) {
                    state.value = state.value.copy(
                        refreshingAccount = false,
                        accountError = SettingsError.AccountRefresh,
                    )
                }
            }
        }
    }
}
