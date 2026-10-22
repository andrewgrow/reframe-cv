package reframecv.ui.components.projects.dashboard

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import reframecv.ui.components.settings.runners.RunnerRunsComponent

class TestDashboardRunnersComponent(initial: DashboardRunnersState = DashboardRunnersState()) :
    DashboardRunnersComponent {
    override val state = MutableValue(initial)
    override val execution =
        MutableValue<ChildSlot<*, RunnerRunsComponent>>(ChildSlot<Unit, RunnerRunsComponent>())
    var selected: Long? = null
    override fun open() {
        state.value = state.value.copy(visible = true)
    }
    override fun select(id: Long) {
        selected = id
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
