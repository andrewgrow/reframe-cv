package reframecv.ui.components.common.ai

import com.arkivanov.decompose.value.MutableValue

class TestAiRequestDialogComponent(
    initial: AiRequestDialogState,
    private val close: () -> Unit = {},
) : AiRequestDialogComponent {
    override val state = MutableValue(initial)
    override fun onConnection(id: Long) {
        state.value = state.value.copy(connectionId = id)
    }
    override fun onPrompt(id: Long) {
        state.value.prompts.find { it.id == id }?.let {
            state.value = state.value.copy(promptId = id, text = it.text)
        }
    }
    override fun onText(text: String) {
        state.value = state.value.copy(text = text)
    }
    override fun onSavePrompt() {
        state.value = state.value.copy(promptSaved = true)
    }
    override fun onRun() {
        state.value =
            state.value.copy(error = "The provider could not complete the request.")
    }
    override fun onRetry() = Unit
    override fun onClose() = close()
}
