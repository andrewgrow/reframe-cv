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
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.ProviderSetupException
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
        if (state.value.connection.configuration !is OpenRouterConfiguration) return
        polling?.cancel()
        polling = scope.launch {
            while (isActive) {
                val draft = state.value
                if (draft.connection.configuration !is OpenRouterConfiguration) {
                    awaitNextRefresh(ACCOUNT_REFRESH_INTERVAL_MILLIS)
                    continue
                }
                val age = draft.connection.openRouterCache.accountInfo?.fetchedAt?.let {
                    now() - it
                }
                val stale = age == null || age < 0 || age >= ACCOUNT_REFRESH_INTERVAL_MILLIS
                if (sameCredentials(draft) && stale && draft.connection.active) refreshBudgets()
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

    private fun sameCredentials(draft: ProviderEditorState) = savedConnection?.let {
        it.id == draft.connection.id && it.type == draft.connection.type &&
            it.openRouter.apiKey == draft.connection.openRouter.apiKey.trim()
    } == true

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
        if (draft.connection.configuration !is OpenRouterConfiguration) return
        val canRefresh =
            lifecycle.state == Lifecycle.State.RESUMED && !draft.busy && draft.connection.active
        if (!canRefresh || draft.refreshingAccount ||
            draft.connection.openRouter.apiKey.isBlank()
        ) {
            return
        }
        val version = ++generation
        state.value =
            draft.copy(refreshingAccount = true, accountError = null, accountErrorDetails = null)
        request = scope.launch {
            try {
                val info = useCases.refreshAccountInfo(draft.connection)
                currentCoroutineContext().ensureActive()
                if (generation == version) {
                    state.value = state.value.copy(
                        connection = state.value.connection.copy(
                            cachedInfo = state.value.connection.openRouterCache.copy(
                                accountInfo = info,
                            ),
                        ),
                        refreshingAccount = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation == version) {
                    state.value = state.value.copy(
                        refreshingAccount = false,
                        accountError = SettingsError.AccountRefresh,
                        accountErrorDetails = (error as? ProviderSetupException)?.providerMessage,
                    )
                }
            }
        }
    }
}
