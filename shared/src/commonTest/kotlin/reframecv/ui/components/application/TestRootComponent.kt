package reframecv.ui.components.application

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.ui.components.application.navigation.ProjectTreeComponent
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.TestProjectsComponent

class TestRootComponent(
    projectsComponent: ProjectsComponent = TestProjectsComponent(),
    private val onProjectsListClick: () -> Unit = {},
) : RootComponent {
    override val projectTree = object : ProjectTreeComponent {
        override val state = MutableValue(ProjectTreeState(loading = false))
        override fun onToggle(id: Long) = Unit
        override fun onProjectSelected(id: Long) = Unit
        override fun onProjectsList() = onProjectsListClick()
        override fun onRetry() = Unit
    }
    override fun onProjectsList() = onProjectsListClick()
    override val childStack: Value<ChildStack<*, RootComponent.Child>> = MutableValue(
        ChildStack(
            configuration = Unit,
            instance = RootComponent.Child.Projects(projectsComponent),
        ),
    )
}
