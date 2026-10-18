package reframecv.ui.components.settings

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException
import reframecv.ui.context.AppComponentContext

/** Identifies safe, localizable connection setup feedback. */
enum class SettingsError {
    Load,
    Save,
    Unauthorized,
    Unavailable,
    InvalidResponse,
    NoModels,
    Validation,
    AccountRefresh,
    Disabled,
}

/** Keeps credentials and model choices in a draft until explicit Save. */
data class ProviderEditorState(
    val connection: ProviderConnection,
    val models: List<AiModel> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: SettingsError? = null,
    val refreshingAccount: Boolean = false,
    val accountError: SettingsError? = null,
    val changingActive: Boolean = false,
    val successfulSaves: Long = 0,
    val refreshingBalance: Boolean = false,
    val balanceError: Boolean = false,
) {
    val busy get() = loading || saving || changingActive
    val valid get() = connection.apiKey.isNotBlank()
}

/** Lets users configure a provider connection without persisting unfinished input. */
interface ProviderEditorComponent {
    val state: Value<ProviderEditorState>
    fun onName(value: String)
    fun onManagementKey(value: String)
    fun onKey(value: String)
    fun onModel(id: String)
    fun onLoadModels()
    fun onSave()
    fun onBack()
    fun onRefreshBalance()
    fun onRefreshAccount()
    fun onActiveChange(active: Boolean)
}

/** Saves connection drafts independently of optional credential checks and model discovery. */
class DefaultProviderEditorComponent(
    context: AppComponentContext,
    connection: ProviderConnection,
    private val saved: () -> Unit,
    private val back: () -> Unit,
) : ProviderEditorComponent {
    private val useCases = context.dependencies.providerSettingsUseCases
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var generation = 0L
    override val state = MutableValue(ProviderEditorState(connection))
    private val account = ProviderAccountController(useCases, context.lifecycle, scope, state)
    private val status = ProviderStatusController(useCases, scope, state) {
        cancelModelLoading()
        account.cancel()
    }

    override fun onRefreshAccount() = account.refreshBudgets()
    override fun onRefreshBalance() = account.refreshBalance()
    override fun onActiveChange(active: Boolean) = status.change(active)
    init {
        context.lifecycle.doOnDestroy {
            generation++
            scope.cancel()
        }
    }

    override fun onName(value: String) {
        if (!state.value.busy) {
            state.value = state.value.copy(
                connection = state.value.connection.copy(name = value),
                error = null,
            )
        }
    }
    override fun onKey(value: String) {
        if (!state.value.busy) {
            account.cancel()
            state.value = state.value.copy(
                connection = state.value.connection.copy(
                    apiKey = value,
                    accountInfo = null,
                ),
                models = emptyList(),
                error = null,
                accountError = null,
            )
        }
    }
    override fun onManagementKey(value: String) {
        if (!state.value.busy) {
            account.cancel()
            state.value = state.value.copy(
                connection = state.value.connection.copy(
                    managementApiKey = value,
                    creditBalance = null,
                ),
                balanceError = false,
            )
        }
    }
    override fun onModel(id: String) {
        if (!state.value.busy && state.value.models.any { it.id == id }) {
            state.value = state.value.copy(connection = state.value.connection.copy(modelId = id))
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onLoadModels() {
        if (state.value.busy || !state.value.connection.active) return
        account.cancel()
        val draft = state.value
        if (draft.connection.apiKey.isBlank()) {
            state.value = draft.copy(error = SettingsError.Validation)
            return
        }
        val request = ++generation
        state.value = draft.copy(loading = true, error = null)
        job = scope.launch {
            try {
                val models = useCases.loadModels(draft.connection)
                currentCoroutineContext().ensureActive()
                if (request == generation) {
                    state.value = draft.copy(
                        models = models,
                        error = if (models.isEmpty()) SettingsError.NoModels else null,
                    )
                    account.refreshBudgets()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (request == generation) {
                    state.value = draft.copy(
                        error = when (
                            (error as? ProviderSetupException)?.reason
                        ) {
                            ProviderSetupError.Unauthorized -> SettingsError.Unauthorized
                            ProviderSetupError.InvalidResponse -> SettingsError.InvalidResponse
                            ProviderSetupError.Disabled -> SettingsError.Disabled
                            else -> SettingsError.Unavailable
                        },
                    )
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onSave() {
        if (state.value.saving || state.value.changingActive) return
        if (state.value.loading) cancelModelLoading()
        account.cancel()
        val draft = state.value
        if (!draft.valid) {
            state.value = draft.copy(error = SettingsError.Validation)
            return
        }
        state.value = draft.copy(saving = true, error = null)
        job = scope.launch {
            try {
                val id = useCases.save(draft.connection)
                val persisted = draft.connection.copy(
                    id = id,
                    name = draft.connection.name.trim(),
                    apiKey = draft.connection.apiKey.trim(),
                    managementApiKey = draft.connection.managementApiKey.trim(),
                    modelId = draft.connection.modelId.trim(),
                )
                state.value = state.value.copy(
                    connection = persisted,
                    saving = false,
                    successfulSaves = state.value.successfulSaves + 1,
                )
                account.onSaved(persisted)
                saved()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = draft.copy(error = SettingsError.Save)
            }
        }
    }
    override fun onBack() {
        if (state.value.saving || state.value.changingActive) return
        if (state.value.loading) {
            cancelModelLoading()
        } else {
            back()
        }
    }

    private fun cancelModelLoading() {
        generation++
        job?.cancel()
        state.value = state.value.copy(loading = false)
    }
}
