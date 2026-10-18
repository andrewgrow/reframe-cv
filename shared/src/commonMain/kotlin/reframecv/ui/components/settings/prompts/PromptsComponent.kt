package reframecv.ui.components.settings.prompts

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.prompts.Prompt
import reframecv.ui.context.AppComponentContext

/** Keeps enabled and disabled prompt records visible in their management list. */
data class PromptsState(
    val prompts: List<Prompt> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

/** Lets settings manage reusable prompts and open their individual editors. */
interface PromptsComponent {
    val state: Value<PromptsState>
    val editorSlot: Value<ChildSlot<*, PromptEditorComponent>>
    fun onAdd()
    fun onEdit(prompt: Prompt)
    fun onRetry()
    fun onBack()
    val isBusy: Boolean get() = editorSlot.value.child?.instance?.state?.value?.saving == true
}

/** Observes persisted prompts while each editor owns an independent draft. */
class DefaultPromptsComponent(context: AppComponentContext, private val back: () -> Unit) :
    PromptsComponent,
    AppComponentContext by context {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null
    override val state = MutableValue(PromptsState())
    private val navigation = SlotNavigation<Prompt>()
    override val editorSlot =
        childSlot(navigation, serializer = null, handleBackButton = false) { prompt, childContext ->
            DefaultPromptEditorComponent(childContext, prompt, back = {
                navigation.dismiss()
            }) as PromptEditorComponent
        }
    init {
        lifecycle.doOnDestroy { scope.cancel() }
        onRetry()
    }
    override fun onAdd() = onEdit(Prompt())
    override fun onEdit(prompt: Prompt) {
        if (!isBusy) navigation.activate(prompt)
    }
    override fun onBack() {
        editorSlot.value.child?.instance?.onBack() ?: back()
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onRetry() {
        observation?.cancel()
        state.value = state.value.copy(loading = true, failed = false)
        observation = scope.launch {
            try {
                dependencies.promptsUseCases.observeAll().collect {
                    state.value = PromptsState(it, loading = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error("Load prompts", error, emptyMap())
                state.value = state.value.copy(loading = false, failed = true)
            }
        }
    }
}
