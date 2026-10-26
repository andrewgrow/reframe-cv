package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue

class TestRunnerEditorComponent(initial: RunnerEditorState) : RunnerEditorComponent {
    override val executions =
        MutableValue<ChildSlot<*, RunnerRunsComponent>>(ChildSlot<Unit, RunnerRunsComponent>())
    override fun onRuns() = Unit
    override val state = MutableValue(initial)
    override val copy = TestRunnerCopyComponent { onDraft(it) }
    var closed = false
    override fun onDraft(draft: RunnerDraft) {
        state.value =
            state.value.copy(draft = draft, error = null)
    }
    override fun onSave() {
        state.value = if (state.value.draft.valid) {
            state.value.copy(successfulSaves = 1)
        } else {
            state.value.copy(error = RunnerEditorError.Validation)
        }
    }
    override fun onDelete() {
        state.value = state.value.copy(confirming = true)
    }
    override fun onConfirmDelete() {
        closed = true
        state.value =
            state.value.copy(confirming = false)
    }
    override fun onBack() {
        if (copy.state.value.visible) {
            copy.dismiss()
        } else if (state.value.confirming) {
            state.value = state.value.copy(confirming = false)
        } else {
            closed = true
        }
    }
}

class TestProviderRunnersComponent(initial: ProviderRunnersState) : ProviderRunnersComponent {
    override val state = MutableValue(initial)
    override val editorSlot =
        MutableValue<ChildSlot<*, RunnerEditorComponent>>(ChildSlot<Unit, RunnerEditorComponent>())
    override fun onAdd() = Unit
    override fun onEdit(id: Long) = Unit
    override fun onRetry() = Unit
    override fun onBack() = Unit
}
