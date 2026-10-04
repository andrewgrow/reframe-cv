package reframecv.ui.components.application

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import reframecv.ui.components.projects.DefaultProjectsComponent
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.context.AppComponentContext

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>
    fun onProjectsList()

    sealed interface Child {
        data class Projects(val component: ProjectsComponent) : Child
    }
}

class DefaultRootComponent(componentContext: AppComponentContext) :
    RootComponent,
    AppComponentContext by componentContext {
    private val navigation = StackNavigation<Configuration>()

    override fun onProjectsList() {
        navigation.bringToFront(Configuration.Projects)
    }

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        // There is only one route for now; each launch opens Projects.
        serializer = null,
        initialConfiguration = Configuration.Projects,
        childFactory = ::createChild,
    )

    private fun createChild(
        configuration: Configuration,
        componentContext: AppComponentContext,
    ): RootComponent.Child = when (configuration) {
        Configuration.Projects -> RootComponent.Child.Projects(
            DefaultProjectsComponent(componentContext),
        )
    }

    private sealed interface Configuration {
        data object Projects : Configuration
    }
}
