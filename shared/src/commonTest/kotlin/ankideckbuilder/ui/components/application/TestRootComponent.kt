package ankideckbuilder.ui.components.application

import ankideckbuilder.ui.components.projects.ProjectsComponent
import ankideckbuilder.ui.components.projects.TestProjectsComponent
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

class TestRootComponent(projectsComponent: ProjectsComponent = TestProjectsComponent()) :
    RootComponent {
    override val childStack: Value<ChildStack<*, RootComponent.Child>> = MutableValue(
        ChildStack(
            configuration = Unit,
            instance = RootComponent.Child.Projects(projectsComponent),
        ),
    )
}
