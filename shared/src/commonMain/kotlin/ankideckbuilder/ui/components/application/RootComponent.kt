package ankideckbuilder.ui.components.application

import ankideckbuilder.ui.components.projects.DefaultProjectsComponent
import ankideckbuilder.ui.components.projects.ProjectsComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>

    sealed interface Child {
        data class Projects(val component: ProjectsComponent) : Child
    }
}

class DefaultRootComponent(componentContext: ComponentContext) :
    RootComponent,
    ComponentContext by componentContext {
    private val navigation = StackNavigation<Configuration>()

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        // There is only one route for now; each launch opens Projects.
        serializer = null,
        initialConfiguration = Configuration.Projects,
        childFactory = ::createChild,
    )

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext,
    ): RootComponent.Child = when (configuration) {
        Configuration.Projects -> RootComponent.Child.Projects(
            DefaultProjectsComponent(componentContext),
        )
    }

    private sealed interface Configuration {
        data object Projects : Configuration
    }
}
