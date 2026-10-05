package reframecv.ui.components.application.navigation

import com.arkivanov.decompose.value.MutableValue

class TestProjectTreeComponent(
    initialState: ProjectTreeState = ProjectTreeState(loading = false),
    private val onSelect: (Long) -> Unit = {},
    private val onRoot: () -> Unit = {},
) : ProjectTreeComponent {
    override val state = MutableValue(initialState)
    override fun onToggle(id: Long) {
        val expanded = state.value.expandedIds
        state.value = state.value.copy(
            expandedIds = if (id in expanded) expanded - id else expanded + id,
        )
    }
    override fun onProjectSelected(id: Long) {
        state.value = state.value.copy(selectedId = id)
        onSelect(id)
    }
    override fun onProjectsList() {
        state.value = state.value.copy(selectedId = null)
        onRoot()
    }
    override fun onRetry() = Unit
}
