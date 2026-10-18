package reframecv.ui.components.common.ai

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ProviderConnection
import reframecv.ui.context.AppComponentContext

/** Holds editable request choices without changing the saved prompt until explicitly requested. */
data class AiRequestDialogState(
    val sourceName: String,
    val connections: List<ProviderConnection> = emptyList(),
    val prompts: List<Prompt> = emptyList(),
    val connectionId: Long? = null,
    val promptId: Long? = null,
    val text: String = "",
    val loading: Boolean = true,
    val choicesFailed: Boolean = false,
    val running: Boolean = false,
    val savingPrompt: Boolean = false,
    val promptSaved: Boolean = false,
    val error: String? = null,
) {
    val connection get() = connections.find { it.id == connectionId }
    val prompt get() = prompts.find { it.id == promptId }
    val canRun get() = !loading && !running && !savingPrompt && prompt != null &&
        text.isNotBlank() && connection?.let {
            it.apiKey.isNotBlank() && it.modelId.isNotBlank()
        } == true
}

/** Exposes an optional request dialog through Decompose's non-null value contract. */
data class AiRequestDialogSlot(val component: AiRequestDialogComponent? = null)

/** Controls a reusable AI request dialog independently of the caller's result type. */
interface AiRequestDialogComponent {
    val state: Value<AiRequestDialogState>
    fun onConnection(id: Long)
    fun onPrompt(id: Long)
    fun onText(text: String)
    fun onSavePrompt()
    fun onRun()
    fun onRetry()
    fun onClose()
}

/** Carries an operation-specific, displayable failure without closing the request dialog. */
class AiRequestException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Selects active services and prompts, owns cancellation, and returns only successful results. */
class DefaultAiRequestDialogComponent<Result>(
    context: AppComponentContext,
    sourceName: String,
    private val component: PromptComponent,
    private val execute: suspend (ProviderConnection, String) -> Result,
    private val completed: (Result) -> Unit,
    private val dismissed: () -> Unit,
) : AiRequestDialogComponent {
    private val dependencies = context.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    private var closed = false
    private var generation = 0L
    override val state = MutableValue(AiRequestDialogState(sourceName))

    init {
        onRetry()
        context.lifecycle.doOnDestroy { dispose() }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRetry() {
        if (closed || state.value.running || state.value.savingPrompt) return
        observation?.cancel()
        state.value = state.value.copy(loading = true, choicesFailed = false, error = null)
        observation = scope.launch {
            try {
                combine(
                    dependencies.providerSettingsUseCases.observe(),
                    dependencies.promptsUseCases.observeAvailable(component),
                ) { connections, prompts -> connections.filter { it.active } to prompts }
                    .collect { (connections, prompts) -> updateChoices(connections, prompts) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                report("Load AI request choices", error, "Could not load providers and prompts.")
                state.value = state.value.copy(loading = false, choicesFailed = true)
            }
        }
    }

    private fun updateChoices(connections: List<ProviderConnection>, prompts: List<Prompt>) {
        val old = state.value
        val connectionId = old.connectionId?.takeIf { id -> connections.any { it.id == id } }
            ?: connections.singleOrNull()?.id
        val promptId = old.promptId?.takeIf { id -> prompts.any { it.id == id } }
            ?: prompts.singleOrNull()?.id
        state.value = old.copy(
            connections = connections,
            prompts = prompts,
            connectionId = connectionId,
            promptId = promptId,
            text = if (promptId == old.promptId) {
                old.text
            } else {
                prompts.find { it.id == promptId }?.text.orEmpty()
            },
            loading = false,
            choicesFailed = false,
        )
    }

    override fun onConnection(id: Long) {
        if (editable() && state.value.connections.any { it.id == id }) {
            state.value = state.value.copy(connectionId = id, error = null)
        }
    }

    override fun onPrompt(id: Long) {
        val prompt = state.value.prompts.find { it.id == id } ?: return
        if (editable()) {
            state.value = state.value.copy(
                promptId = id,
                text = prompt.text,
                error = null,
                promptSaved = false,
            )
        }
    }

    override fun onText(text: String) {
        if (editable()) state.value = state.value.copy(text = text, promptSaved = false)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onSavePrompt() {
        val draft = state.value
        if (!editable() || draft.text.isBlank()) return
        val prompt = draft.prompt ?: return
        state.value = draft.copy(savingPrompt = true, promptSaved = false, error = null)
        scope.launch {
            try {
                val current = dependencies.promptsUseCases.observeAvailable(component).first()
                    .find { it.id == prompt.id } ?: error("Prompt is no longer active")
                dependencies.promptsUseCases.save(current.copy(text = draft.text))
                currentCoroutineContext().ensureActive()
                if (!closed) state.value = state.value.copy(promptSaved = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                report("Save AI request prompt", error, "Could not save the prompt.")
            } finally {
                if (!closed) state.value = state.value.copy(savingPrompt = false)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRun() {
        val draft = state.value
        if (closed || !draft.canRun) return
        val id = draft.connectionId ?: return
        val request = ++generation
        state.value = draft.copy(running = true, error = null, promptSaved = false)
        dependencies.eventHistory.record(
            "AI request started(connectionId=$id, promptId=${draft.promptId})",
        )
        scope.launch {
            try {
                check(
                    dependencies.promptsUseCases.observeAvailable(component).first()
                        .any { it.id == draft.promptId },
                ) { "Prompt is no longer active" }
                val connection = dependencies.providerSettingsUseCases.requireUsableConnection(id)
                val result = execute(connection, draft.text)
                currentCoroutineContext().ensureActive()
                dependencies.providerSettingsUseCases.requireUsableConnection(id)
                if (!closed && request == generation) {
                    dispose()
                    completed(result)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!closed && request == generation) {
                    report(
                        "Execute AI request",
                        error,
                        "The AI request failed. Check the provider and prompt, then retry.",
                    )
                }
            } finally {
                if (!closed && request == generation) {
                    state.value = state.value.copy(running = false)
                }
            }
        }
    }

    private fun editable() = !closed && !state.value.running && !state.value.savingPrompt

    private fun report(operation: String, error: Exception, fallback: String) {
        if (closed) return
        dependencies.logger.error(
            operation,
            error,
            mapOf(
                "connectionId" to state.value.connectionId,
                "promptId" to state.value.promptId,
            ),
        )
        state.value = state.value.copy(error = (error as? AiRequestException)?.message ?: fallback)
    }

    override fun onClose() {
        if (closed) return
        dispose()
        dismissed()
    }

    private fun dispose() {
        closed = true
        generation++
        scope.cancel()
    }
}
