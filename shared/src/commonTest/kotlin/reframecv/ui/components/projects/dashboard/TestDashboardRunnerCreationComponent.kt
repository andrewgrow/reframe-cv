package reframecv.ui.components.projects.dashboard

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import reframecv.ui.components.settings.runners.RunnerEditorComponent

class TestDashboardRunnerCreationComponent(
    initial: DashboardRunnerCreationState = DashboardRunnerCreationState(),
) : DashboardRunnerCreationComponent {
    override val state = MutableValue(initial)
    override val editor =
        MutableValue<ChildSlot<*, RunnerEditorComponent>>(ChildSlot<Unit, RunnerEditorComponent>())
    var selected: Long? = null
    override fun open() {
        state.value = state.value.copy(visible = true)
    }
    override fun select(connectionId: Long) {
        selected = connectionId
        dismiss()
    }
    override fun dismiss() {
        state.value = state.value.copy(visible = false)
    }
    override fun onBack(): Boolean {
        val visible = state.value.visible
        dismiss()
        return visible
    }
}
