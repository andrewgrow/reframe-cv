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
}

/** Keeps credentials and model choices in a draft until explicit Save. */
data class ProviderEditorState(
    val connection: ProviderConnection,
    val models: List<AiModel> = emptyList(),
    val verified: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: SettingsError? = null,
) {
    val busy get() = loading || saving
    val valid get() = connection.name.isNotBlank() && connection.apiKey.isNotBlank() &&
        verified && models.any { it.id == connection.modelId }
}

/** Lets users configure a provider connection without persisting unfinished input. */
interface ProviderEditorComponent {
    val state: Value<ProviderEditorState>
    fun onName(value: String)
    fun onKey(value: String)
    fun onModel(id: String)
    fun onLoadModels()
    fun onSave()
    fun onBack()
}

/** Validates credentials, retains failed drafts, and saves a selected model explicitly. */
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
    override val state = MutableValue(
        ProviderEditorState(
            connection,
            models = if (connection.id ==
                0L
            ) {
                emptyList()
            } else {
                listOf(AiModel(connection.modelId, connection.modelId))
            },
            verified = connection.id != 0L,
        ),
    )
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
            state.value = state.value.copy(
                connection = state.value.connection.copy(apiKey = value, modelId = ""),
                models = emptyList(),
                verified = false,
                error = null,
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
        val draft = state.value
        if (draft.busy) return
        if (draft.connection.apiKey.isBlank()) {
            state.value = draft.copy(error = SettingsError.Validation)
            return
        }
        val request = ++generation
        state.value = draft.copy(loading = true, error = null)
        job = scope.launch {
            try {
                val models = useCases.loadModels(draft.connection.type, draft.connection.apiKey)
                currentCoroutineContext().ensureActive()
                if (request == generation) {
                    state.value = draft.copy(
                        connection = draft.connection.copy(
                            modelId = draft.connection.modelId.takeIf { id ->
                                models.any { it.id == id }
                            }.orEmpty(),
                        ),
                        models = models,
                        verified = true,
                        error = if (models.isEmpty()) SettingsError.NoModels else null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (request == generation) {
                    state.value = draft.copy(
                        verified = false,
                        error = when (
                            (error as? ProviderSetupException)?.reason
                        ) {
                            ProviderSetupError.Unauthorized -> SettingsError.Unauthorized
                            ProviderSetupError.InvalidResponse -> SettingsError.InvalidResponse
                            else -> SettingsError.Unavailable
                        },
                    )
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onSave() {
        val draft = state.value
        if (draft.busy) return
        if (!draft.valid) {
            state.value = draft.copy(error = SettingsError.Validation)
            return
        }
        state.value = draft.copy(saving = true, error = null)
        job = scope.launch {
            try {
                useCases.save(draft.connection)
                saved()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = draft.copy(error = SettingsError.Save)
            }
        }
    }
    override fun onBack() {
        if (state.value.saving) return
        if (state.value.loading) {
            generation++
            job?.cancel()
            state.value = state.value.copy(loading = false)
        } else {
            back()
        }
    }
}
