package reframecv.ui.components.application

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.TestProjectsComponent

class TestRootComponent(
    projectsComponent: ProjectsComponent = TestProjectsComponent(),
    private val onProjectsListClick: () -> Unit = {},
) : RootComponent {
    override fun onProjectsList() = onProjectsListClick()
    override val childStack: Value<ChildStack<*, RootComponent.Child>> = MutableValue(
        ChildStack(
            configuration = Unit,
            instance = RootComponent.Child.Projects(projectsComponent),
        ),
    )
}
