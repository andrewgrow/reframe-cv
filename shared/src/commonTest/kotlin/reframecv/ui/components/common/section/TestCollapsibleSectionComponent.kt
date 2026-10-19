package reframecv.ui.components.common.section

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.screenstate.ScreenStateOwner

class TestCollapsibleSectionComponent(
    initialState: CollapsibleSectionState = CollapsibleSectionState(),
) : CollapsibleSectionComponent {
    override val state = MutableValue(initialState)
    override fun onToggle() {
        if (!state.value.loading) state.value = state.value.copy(expanded = !state.value.expanded)
    }
    override fun onBindOwner(owner: ScreenStateOwner) = Unit
}
