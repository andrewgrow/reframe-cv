package reframecv.ui.components.application

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import reframecv.domain.models.project.Project
import reframecv.ui.components.application.navigation.DefaultProjectTreeComponent
import reframecv.ui.components.application.navigation.ProjectTreeComponent
import reframecv.ui.components.projects.DefaultProjectsComponent
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.context.AppComponentContext

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>
    val projectTree: ProjectTreeComponent
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

    override val projectTree = DefaultProjectTreeComponent(
        componentContext = componentContext,
        openProject = { projects -> openPath(projects.map { ProjectBreadcrumb(it.id, it.name) }) },
        openProjectsList = ::onProjectsList,
        onProjectsChanged = ::onProjectsChanged,
    )

    init {
        val updateSelection: (ChildStack<*, RootComponent.Child>) -> Unit = { stack ->
            val child = stack.active.instance as RootComponent.Child.Projects
            projectTree.select(child.component.projectPath.lastOrNull()?.id)
        }
        val subscription = childStack.subscribe(updateSelection)
        lifecycle.doOnDestroy { subscription.cancel() }
    }

    private fun openPath(path: List<ProjectBreadcrumb>) {
        val configurations = childStack.value.items.map {
            it.configuration as Configuration.Projects
        }
        val existing = configurations.indexOfLast { candidate ->
            candidate.projectPath.map { it.id } == path.map { it.id }
        }
        if (existing >= 0) {
            navigation.popTo(existing)
        } else {
            navigation.pushNew(Configuration.Projects(path))
        }
    }

    private fun onProjectsChanged(projects: List<Project>) {
        val current = (childStack.value.active.configuration as Configuration.Projects).projectPath
        val activeIds = projects.map { it.id }.toSet()
        if (current.any { it.id !in activeIds }) {
            openPath(current.takeWhile { it.id in activeIds })
        }
        navigation.navigate(transformer = { stack ->
            stack.filter { configuration ->
                (configuration as Configuration.Projects).projectPath.all { it.id in activeIds }
            }
        }, onComplete = { _, _ -> })
    }

    private fun createChild(
        configuration: Configuration,
        componentContext: AppComponentContext,
    ): RootComponent.Child = when (configuration) {
        is Configuration.Projects -> RootComponent.Child.Projects(
            DefaultProjectsComponent(
                componentContext,
                projectPath = configuration.projectPath,
                onProjectOpened = { project ->
                    openPath(
                        configuration.projectPath + ProjectBreadcrumb(project.id, project.name),
                    )
                },
                onBreadcrumbSelected = { index ->
                    openPath(configuration.projectPath.take(index))
                },
            ),
        )
    }

    private sealed interface Configuration {
        data class Projects(val projectPath: List<ProjectBreadcrumb> = emptyList()) : Configuration
    }
}
