package ankideckbuilder.ui.components.projects

import ankideckbuilder.ui.components.projects.editor.EditorComponent
import ankideckbuilder.ui.components.projects.editor.TestEditorComponent
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue

class TestProjectsComponent(
    initialState: UiState = UiState.NoProjects,
    editorInitiallyOpen: Boolean = false,
    private val onAddProjectClick: () -> Unit = {},
) : ProjectsComponent {
    override val uiState = MutableValue(initialState)
    override val editorSlot =
        MutableValue<ChildSlot<*, EditorComponent>>(
            if (editorInitiallyOpen) createEditorSlot() else ChildSlot(),
        )

    override fun onAddProject() {
        if (editorSlot.value.child == null) {
            editorSlot.value = createEditorSlot()
        }
        onAddProjectClick()
    }

    private fun createEditorSlot(): ChildSlot<Unit, EditorComponent> = ChildSlot(
        child = Child.Created(
            Unit,
            TestEditorComponent {
                editorSlot.value = ChildSlot<Unit, EditorComponent>()
            },
        ),
    )
}
