package reframecv.ui.components.settings

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import reframecv.domain.models.providers.ProviderConnection
import reframecv.usecase.ProviderSettingsUseCases

/** Refreshes optional account credits independently of ordinary key budgets and model discovery. */
internal class ProviderBalanceController(
    private val useCases: ProviderSettingsUseCases,
    private val lifecycle: Lifecycle,
    private val scope: CoroutineScope,
    private val state: MutableValue<ProviderEditorState>,
) {
    private var request: Job? = null
    private var generation = 0L

    fun refreshIfStale(saved: ProviderConnection?, now: Long): Long {
        val connection = state.value.connection
        val matches = saved?.let {
            it.id == connection.id && it.type == connection.type &&
                it.managementApiKey == connection.managementApiKey.trim()
        } == true
        if (!matches || !connection.active || connection.managementApiKey.isBlank()) {
            return ACCOUNT_REFRESH_INTERVAL_MILLIS
        }
        val age = connection.creditBalance?.fetchedAt?.let { now - it }
        return if (age == null || age < 0 || age >= ACCOUNT_REFRESH_INTERVAL_MILLIS) {
            refresh()
            ACCOUNT_REFRESH_INTERVAL_MILLIS
        } else {
            ACCOUNT_REFRESH_INTERVAL_MILLIS - age
        }
    }

    fun cancel() {
        generation++
        request?.cancel()
        request = null
        state.value = state.value.copy(refreshingBalance = false)
    }

    @Suppress("TooGenericExceptionCaught")
    fun refresh() {
        val draft = state.value
        val canRefresh =
            lifecycle.state == Lifecycle.State.RESUMED && !draft.busy && draft.connection.active
        if (!canRefresh || draft.refreshingBalance || draft.connection.managementApiKey.isBlank()) {
            return
        }
        val version = ++generation
        state.value = draft.copy(refreshingBalance = true, balanceError = false)
        request = scope.launch {
            try {
                val balance = useCases.refreshCreditBalance(draft.connection)
                currentCoroutineContext().ensureActive()
                if (version == generation) {
                    state.value = state.value.copy(
                        connection = state.value.connection.copy(creditBalance = balance),
                        refreshingBalance = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (version == generation) {
                    state.value = state.value.copy(refreshingBalance = false, balanceError = true)
                }
            }
        }
    }
}
