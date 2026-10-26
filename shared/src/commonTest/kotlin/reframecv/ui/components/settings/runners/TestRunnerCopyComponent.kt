package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue

class TestRunnerCopyComponent(private val copied: (RunnerDraft) -> Unit) : RunnerCopyComponent {
    override val state = MutableValue(RunnerCopyState())
    override fun open() {
        state.value = state.value.copy(visible = true, draft = null)
    }
    override fun select(id: Long) {
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
        state.value = state.value.copy(draft = draft)
    }
    override fun apply() {
        val draft = state.value.draft ?: return
        dismiss()
        copied(draft)
    }
    override fun dismiss() {
        state.value = state.value.copy(visible = false, draft = null)
    }
}
