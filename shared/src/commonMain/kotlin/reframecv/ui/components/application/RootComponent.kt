package reframecv.ui.components.application

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import reframecv.ui.components.projects.DefaultProjectsComponent
import reframecv.ui.components.projects.ProjectBreadcrumb
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
        navigation.popTo(0)
    }

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        // Each launch starts at the root project list.
        serializer = null,
        initialConfiguration = Configuration.Projects(),
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(
        configuration: Configuration,
        componentContext: AppComponentContext,
    ): RootComponent.Child = when (configuration) {
        is Configuration.Projects -> RootComponent.Child.Projects(
            DefaultProjectsComponent(
                componentContext,
                projectPath = configuration.projectPath,
                onProjectOpened = { project ->
                    navigation.pushNew(
                        Configuration.Projects(
                            configuration.projectPath + ProjectBreadcrumb(project.id, project.name),
                        ),
                    )
                },
                onBreadcrumbSelected = { index -> navigation.popTo(index) },
            ),
        )
    }

    private sealed interface Configuration {
        data class Projects(val projectPath: List<ProjectBreadcrumb> = emptyList()) : Configuration
    }
}
