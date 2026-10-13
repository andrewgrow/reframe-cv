package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent

class TestResumesComponent(
    override val projectId: Long = 1,
    initialState: ResumesState = ResumesState.Ready(emptyList()),
    private val retry: () -> Unit = {},
    initialEditor: ResumeEditorComponent? = null,
    private val add: () -> Unit = {},
    private val back: () -> Unit = {},
) : ResumesComponent {
    override val childStack = MutableValue(
        ChildStack<Any, ResumesComponent.Child>(
            active = Child.Created(
                "page",
                initialEditor?.let { ResumesComponent.Child.Editor(it) }
                    ?: ResumesComponent.Child.List,
            ),
            backStack = emptyList(),
        ),
    )
    override fun onAdd() = add()
    override val uiState = MutableValue(initialState)
    override fun onRetry() = retry()
    override fun onBack() = back()
}
