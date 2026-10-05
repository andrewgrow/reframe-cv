package reframecv.ui.components.projects

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.project.Project
import reframecv.ui.components.projects.editor.EditorComponent
import reframecv.ui.components.projects.editor.TestEditorComponent

class TestProjectsComponent(
    initialState: UiState = UiState.NoProjects,
    editorInitiallyOpen: Boolean = false,
    override val projectPath: List<ProjectBreadcrumb> = emptyList(),
    private val onProjectOpened: (Project) -> Unit = {},
    private val onAddProjectClick: () -> Unit = {},
    private val onConfigureProjectClick: () -> Unit = {},
) : ProjectsComponent {
    override val uiState = MutableValue(initialState)
    override val editorSlot =
        MutableValue<ChildSlot<*, EditorComponent>>(
            if (editorInitiallyOpen) createEditorSlot() else ChildSlot(),
        )

    override fun onConfigureProject() = onConfigureProjectClick()

    override fun onAddProject() {
        if (editorSlot.value.child == null) {
            editorSlot.value = createEditorSlot()
        }
        onAddProjectClick()
    }

    override fun onOpenProject(project: Project) = onProjectOpened(project)

    override fun onEditProject(project: Project) {
        editorSlot.value = createEditorSlot(project.name)
    }

    private fun createEditorSlot(initialName: String? = null): ChildSlot<Unit, EditorComponent> =
        ChildSlot(
            child = Child.Created(
                Unit,
                TestEditorComponent(initialName) {
                    editorSlot.value = ChildSlot<Unit, EditorComponent>()
                },
            ),
        )
}
