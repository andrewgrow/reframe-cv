package reframecv.ui.components.settings.prompts

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent

class TestPromptsComponent(initial: PromptsState, editor: PromptEditorState? = null) :
    PromptsComponent {
    override val state = MutableValue(initial)
    override val editorSlot = MutableValue<ChildSlot<*, PromptEditorComponent>>(
        editor?.let { ChildSlot(child = Child.Created(Unit, TestPromptEditorComponent(it))) }
            ?: ChildSlot<Unit, PromptEditorComponent>(),
    )
    override fun onAdd() = Unit
    override fun onEdit(prompt: Prompt) = Unit
    override fun onRetry() = Unit
    override fun onBack() = Unit
}

class TestPromptEditorComponent(initial: PromptEditorState) : PromptEditorComponent {
    override val state = MutableValue(initial)
    override fun onTitle(value: String) {
        state.value =
            state.value.copy(prompt = state.value.prompt.copy(title = value))
    }
    override fun onComponent(value: PromptComponent?) {
        state.value =
            state.value.copy(prompt = state.value.prompt.copy(component = value))
    }
    override fun onText(value: String) {
        state.value =
            state.value.copy(prompt = state.value.prompt.copy(text = value))
    }
    override fun onActive(value: Boolean) {
        state.value =
            state.value.copy(prompt = state.value.prompt.copy(active = value))
    }
    override fun onSave() = Unit
    override fun onBack() = Unit
}
