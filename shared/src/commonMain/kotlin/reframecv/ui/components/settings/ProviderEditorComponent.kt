package reframecv.ui.components.settings

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
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
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.InvalidProviderConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.normalized
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException
import reframecv.ui.components.settings.runners.DefaultProviderRunnersComponent
import reframecv.ui.components.settings.runners.ProviderRunnersComponent
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
    val errorDetails: String? = null,
    val accountErrorDetails: String? = null,
    val balanceErrorDetails: String? = null,
) {
    val busy get() = loading || saving || changingActive
    val valid get() = connection.usable
}

/** Lets users configure a provider connection without persisting unfinished input. */
interface ProviderEditorComponent {
    val runners: Value<ChildSlot<*, ProviderRunnersComponent>>
    val isBusy: Boolean get() = state.value.busy || runners.value.child?.instance?.isBusy == true
    fun onType(type: ApiProviderType) {}
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
    private val runnersNavigation = SlotNavigation<Long>()
    override val runners = context.childSlot(
        runnersNavigation,
        serializer = null,
        key = "ProviderRunners",
        handleBackButton = false,
    ) { id, childContext ->
        DefaultProviderRunnersComponent(childContext, id) as ProviderRunnersComponent
    }
    private val account = ProviderAccountController(useCases, context.lifecycle, scope, state)
    private val status = ProviderStatusController(useCases, scope, state) {
        cancelModelLoading()
        account.cancel()
    }

    override fun onRefreshAccount() = account.refreshBudgets()
    override fun onRefreshBalance() = account.refreshBalance()
    override fun onActiveChange(active: Boolean) = status.change(active)
    init {
        if (connection.id != 0L && connection.type == ApiProviderType.Apify) {
            runnersNavigation.activate(connection.id)
        }
        context.lifecycle.doOnDestroy {
            generation++
            scope.cancel()
        }
    }

    override fun onType(type: ApiProviderType) {
        if (state.value.busy || state.value.connection.id != 0L) return
        if (state.value.connection.type == type) return
        account.cancel()
        val configuration = when (type) {
            ApiProviderType.OpenRouter -> OpenRouterConfiguration()
            ApiProviderType.Apify -> ApifyConfiguration()
        }
        state.value = ProviderEditorState(
            state.value.connection.copy(
                type = type,
                configuration = configuration,
                cachedInfo = null,
            ),
        )
    }
    override fun onName(value: String) {
        if (!state.value.busy) {
            state.value = state.value.copy(
                connection = state.value.connection.copy(name = value),
                error = null,
                errorDetails = null,
            )
        }
    }
    override fun onKey(value: String) {
        if (!state.value.busy) {
            account.cancel()
            state.value = state.value.copy(
                connection = state.value.connection.let { connection ->
                    when (val config = connection.configuration) {
                        is OpenRouterConfiguration -> connection.copy(
                            configuration = config.copy(apiKey = value),
                            cachedInfo = connection.openRouterCache.copy(accountInfo = null),
                        )

                        is ApifyConfiguration -> connection.copy(
                            configuration = config.copy(apiToken = value),
                        )

                        is InvalidProviderConfiguration -> connection
                    }
                },
                models = emptyList(),
                error = null,
                errorDetails = null,
                accountError = null,
                accountErrorDetails = null,
            )
        }
    }
    override fun onManagementKey(value: String) {
        if (state.value.connection.configuration !is OpenRouterConfiguration) return
        if (!state.value.busy) {
            account.cancel()
            state.value = state.value.copy(
                connection = state.value.connection.copy(
                    configuration = state.value.connection.openRouter.copy(
                        managementApiKey = value,
                    ),
                    cachedInfo = state.value.connection.openRouterCache.copy(creditBalance = null),
                ),
                balanceError = false,
                balanceErrorDetails = null,
            )
        }
    }
    override fun onModel(id: String) {
        if (!state.value.busy && state.value.models.any { it.id == id }) {
            state.value =
                state.value.copy(
                    connection = state.value.connection.copy(
                        configuration = state.value.connection.openRouter.copy(modelId = id),
                    ),
                )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onLoadModels() {
        val available = !state.value.busy && state.value.connection.active
        if (!available || state.value.connection.configuration !is OpenRouterConfiguration) return
        account.cancel()
        val draft = state.value
        if (draft.connection.openRouter.apiKey.isBlank()) {
            state.value = draft.copy(error = SettingsError.Validation, errorDetails = null)
            return
        }
        val request = ++generation
        state.value = draft.copy(loading = true, error = null, errorDetails = null)
        job = scope.launch {
            try {
                val models = useCases.loadModels(draft.connection)
                currentCoroutineContext().ensureActive()
                if (request == generation) {
                    state.value = draft.copy(
                        models = models,
                        error = if (models.isEmpty()) SettingsError.NoModels else null,
                        errorDetails = null,
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
                        errorDetails = (error as? ProviderSetupException)?.providerMessage,
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
            state.value = draft.copy(error = SettingsError.Validation, errorDetails = null)
            return
        }
        state.value = draft.copy(saving = true, error = null, errorDetails = null)
        job = scope.launch {
            try {
                val id = useCases.save(draft.connection)
                val persisted = draft.connection.copy(
                    id = id,
                    name = draft.connection.name.trim(),
                    configuration = draft.connection.configuration.normalized(),
                )
                state.value = state.value.copy(
                    connection = persisted,
                    saving = false,
                    successfulSaves = state.value.successfulSaves + 1,
                )
                account.onSaved(persisted)
                if (persisted.type == ApiProviderType.Apify && runners.value.child == null) {
                    runnersNavigation.activate(id)
                }
                saved()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = draft.copy(error = SettingsError.Save, errorDetails = null)
            }
        }
    }
    override fun onBack() {
        val child = runners.value.child?.instance
        if (child?.editorSlot?.value?.child != null) {
            child.onBack()
            return
        }
        when {
            state.value.saving || state.value.changingActive -> Unit
            state.value.loading -> cancelModelLoading()
            else -> back()
        }
    }

    private val cancelModelLoading: () -> Unit = {
        generation++
        job?.cancel()
        state.value = state.value.copy(loading = false)
    }
}
