package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.logging.ApplicationLogger
import reframecv.usecase.ConnectionRunnersUseCases
import reframecv.usecase.SavedRunner

/** Holds copied settings separately until the user applies them to the new runner. */
data class RunnerCopyState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val sources: List<SavedRunner> = emptyList(),
    val draft: RunnerDraft? = null,
)

/** Selects and edits a saved runner's settings without copying its identity or history. */
interface RunnerCopyComponent {
    val state: Value<RunnerCopyState>
    fun open()
    fun select(id: Long)
    fun edit(draft: RunnerDraft)
    fun apply()
    fun dismiss()
}

/** Copies supported operations into a new draft while preserving its owning connection. */
internal class DefaultRunnerCopyComponent(
    private val useCases: ConnectionRunnersUseCases,
    private val scope: CoroutineScope,
    private val logger: ApplicationLogger,
    private val current: () -> RunnerEditorState,
    private val copied: (RunnerDraft) -> Unit,
) : RunnerCopyComponent {
    override val state = MutableValue(RunnerCopyState())
    private var observation: Job? = null

    @Suppress("TooGenericExceptionCaught")
    override fun open() {
        val editor = current()
        if (editor.runner.id != 0L || editor.busy || editor.confirming) return
        observation?.cancel()
        state.value = RunnerCopyState(visible = true, loading = true)
        observation = scope.launch {
            try {
                useCases.observeSaved().collect { sources ->
                    state.value = state.value.copy(
                        loading = false,
                        failed = false,
                        sources = sources.filter {
                            it.connection.type == ApiProviderType.Apify &&
                                RunnerConfigurationJson.decode(it.runner) != null
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error("Load runner copy sources", error, emptyMap())
                state.value = state.value.copy(loading = false, failed = true)
            }
        }
    }
    override fun select(id: Long) {
        if (!state.value.visible || state.value.loading || state.value.failed) return
        val source = state.value.sources.find { it.runner.id == id } ?: return
        val draft = RunnerDraft.from(source.runner)
        state.value = state.value.copy(
            draft = draft.copy(
                name = draft.name.takeIf {
                    it.isNotBlank()
                }?.let { "$it (copy)" }.orEmpty(),
            ),
        )
    }
    override fun edit(draft: RunnerDraft) {
        if (state.value.visible && state.value.draft != null) {
            state.value = state.value.copy(draft = draft)
        }
    }
    override fun apply() {
        val draft = state.value.draft ?: return
        val editor = current()
        if (!state.value.visible || editor.busy || editor.runner.id != 0L) return
        dismiss()
        copied(draft)
    }
    override fun dismiss() {
        observation?.cancel()
        state.value = RunnerCopyState()
    }
}
