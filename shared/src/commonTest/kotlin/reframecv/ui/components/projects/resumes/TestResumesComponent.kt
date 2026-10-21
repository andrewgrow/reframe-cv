package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.resume.Resume
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent

class TestResumesComponent(
    override val projectId: Long = 1,
    initialState: ResumesState = ResumesState.Ready(emptyList()),
    private val retry: () -> Unit = {},
    initialEditor: ResumeEditorComponent? = null,
    private val edit: (Resume) -> Unit = {},
    private val add: () -> Unit = {},
    private val back: () -> Unit = {},
    private val openVacancy: (Long) -> Unit = {},
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
    override fun onEdit(resume: Resume) = edit(resume)
    override fun onAdd() = add()
    override val uiState = MutableValue(initialState)
    override val selection = TestResumeSelectionComponent { uiState.value }
    override fun onOpenVacancy(id: Long) = openVacancy(id)
    override fun onRetry() = retry()
    override fun onBack() = back()
}
