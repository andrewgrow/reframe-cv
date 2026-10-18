package reframecv.ui.components.settings.prompts

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.ui.context.AppComponentContext

/** Identifies prompt validation and persistence failures for local feedback. */
enum class PromptEditorError { Validation, Save }

/** Holds unsaved instructions and the result of the latest save. */
data class PromptEditorState(
    val prompt: Prompt,
    val saving: Boolean = false,
    val error: PromptEditorError? = null,
    val successfulSaves: Long = 0,
)

/** Lets users edit reusable AI instructions without committing unfinished changes. */
interface PromptEditorComponent {
    val state: Value<PromptEditorState>
    fun onTitle(value: String)
    fun onComponent(value: PromptComponent?)
    fun onText(value: String)
    fun onActive(value: Boolean)
    fun onSave()
    fun onBack()
}

/** Saves prompt drafts in place and preserves their identity across subsequent saves. */
class DefaultPromptEditorComponent(
    context: AppComponentContext,
    prompt: Prompt,
    private val back: () -> Unit,
) : PromptEditorComponent {
    private val useCases = context.dependencies.promptsUseCases
    private val logger = context.dependencies.logger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(PromptEditorState(prompt))
    init {
        context.lifecycle.doOnDestroy { scope.cancel() }
    }

    override fun onTitle(value: String) = edit { copy(title = value) }
    override fun onComponent(value: PromptComponent?) = edit { copy(component = value) }
    override fun onText(value: String) = edit { copy(text = value) }
    override fun onActive(value: Boolean) = edit { copy(active = value) }

    private fun edit(change: Prompt.() -> Prompt) {
        if (!state.value.saving) {
            state.value =
                state.value.copy(prompt = state.value.prompt.change(), error = null)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onSave() {
        val draft = state.value
        if (draft.saving) return
        if (draft.prompt.text.isBlank()) {
            state.value = draft.copy(error = PromptEditorError.Validation)
            return
        }
        state.value = draft.copy(saving = true, error = null)
        scope.launch {
            try {
                val id = useCases.save(draft.prompt)
                state.value = draft.copy(
                    prompt = draft.prompt.copy(
                        id = id,
                        title = draft.prompt.title.trim(),
                        text = draft.prompt.text.trim(),
                    ),
                    successfulSaves = draft.successfulSaves + 1,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error("Save prompt", error, mapOf("promptId" to draft.prompt.id))
                state.value = draft.copy(error = PromptEditorError.Save)
            }
        }
    }

    override fun onBack() {
        if (!state.value.saving) back()
    }
}
