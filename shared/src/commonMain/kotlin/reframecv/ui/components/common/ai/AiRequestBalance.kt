package reframecv.ui.components.common.ai

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import reframecv.domain.models.providers.ProviderCreditBalance
import reframecv.logging.ApplicationLogger
import reframecv.usecase.ProviderSettingsUseCases

private const val BALANCE_NOTICE_MILLIS = 5000L

/** Keeps the transient account balance optional within Decompose's non-null state contract. */
data class AiBalanceNotice(val balance: ProviderCreditBalance? = null)

/** Refreshes the used AI account in the background and briefly exposes its remaining balance. */
class AiRequestBalance(
    private val providers: ProviderSettingsUseCases,
    private val logger: ApplicationLogger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val awaitExpiry: suspend () -> Unit = { delay(BALANCE_NOTICE_MILLIS) },
) {
    private val notice = MutableValue(AiBalanceNotice())
    val state: Value<AiBalanceNotice> = notice
    private var refresh: Job? = null
    private var expiry: Job? = null

    @Suppress("TooGenericExceptionCaught")
    fun onRequestFinished(connectionId: Long) {
        refresh?.cancel()
        refresh = scope.launch {
            try {
                val connection = providers.observe().first().find { it.id == connectionId }
                if (connection == null || !connection.active ||
                    connection.managementApiKey.isBlank()
                ) {
                    return@launch
                }
                val balance = providers.refreshCreditBalance(connection)
                val current = providers.observe().first().find { it.id == connectionId }
                if (current?.active != true ||
                    current.managementApiKey != connection.managementApiKey ||
                    current.type != connection.type
                ) {
                    return@launch
                }
                notice.value = AiBalanceNotice(balance)
                expiry?.cancel()
                expiry = scope.launch {
                    awaitExpiry()
                    notice.value = AiBalanceNotice()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error(
                    "Refresh balance after AI request",
                    error,
                    mapOf("connectionId" to connectionId),
                )
            }
        }
    }

    fun close() = scope.cancel()
}
