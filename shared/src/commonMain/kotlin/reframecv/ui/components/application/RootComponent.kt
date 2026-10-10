package reframecv.ui.components.application

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode
import reframecv.ui.components.application.navigation.DefaultProjectTreeComponent
import reframecv.ui.components.application.navigation.ProjectTreeComponent
import reframecv.ui.components.projects.DefaultProjectsComponent
import reframecv.ui.components.projects.ProjectBreadcrumb
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.dashboard.DashboardComponent
import reframecv.ui.components.projects.dashboard.DefaultDashboardComponent
import reframecv.ui.components.projects.vacancies.DefaultVacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.context.AppComponentContext

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>
    val projectTree: ProjectTreeComponent
    fun onProjectsList()

    sealed interface Child {
        data class Projects(val component: ProjectsComponent) : Child
        data class Dashboard(val component: DashboardComponent) : Child
        data class Vacancies(val component: VacanciesComponent) : Child
    }
}

class DefaultRootComponent(componentContext: AppComponentContext) :
    RootComponent,
    AppComponentContext by componentContext {
    private val navigation = StackNavigation<Configuration>()
    private var activeProjects: List<Project> = emptyList()

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
            val configuration = stack.active.configuration as Configuration
            projectTree.select(configuration.projectPath.lastOrNull()?.id)
        }
        val subscription = childStack.subscribe(updateSelection)
        lifecycle.doOnDestroy { subscription.cancel() }
    }

    private fun openPath(
        path: List<ProjectBreadcrumb>,
        mode: ProjectMode? = activeProjects.find { it.id == path.lastOrNull()?.id }?.mode,
    ) {
        val destination = if (mode == ProjectMode.Workspace) {
            Configuration.Dashboard(path)
        } else {
            Configuration.Projects(path)
        }
        val configurations = childStack.value.items.map {
            it.configuration as Configuration
        }
        val existing = configurations.indexOfLast { candidate ->
            candidate::class == destination::class &&
                candidate.projectPath.map { it.id } == path.map { it.id }
        }
        if (existing >= 0) {
            navigation.popTo(existing)
        } else {
            navigation.pushNew(destination)
        }
    }

    private fun onProjectsChanged(projects: List<Project>) {
        activeProjects = projects
        val current = (childStack.value.active.configuration as Configuration).projectPath
        val activeIds = projects.map { it.id }.toSet()
        if (current.any { it.id !in activeIds }) {
            openPath(current.takeWhile { it.id in activeIds })
        }
        navigation.navigate(transformer = { stack ->
            stack.filter { configuration ->
                configuration.projectPath.all { it.id in activeIds }
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
                onProjectConfigured = {
                    navigation.pushNew(Configuration.Dashboard(configuration.projectPath))
                },
                onProjectOpened = { project ->
                    openPath(
                        configuration.projectPath + ProjectBreadcrumb(project.id, project.name),
                        project.mode,
                    )
                },
            ),
        )

        is Configuration.Dashboard -> RootComponent.Child.Dashboard(
            DefaultDashboardComponent(
                componentContext,
                configuration.projectPath.last().id,
                openVacancies = {
                    navigation.pushNew(Configuration.Vacancies(configuration.projectPath))
                },
            ),
        )

        is Configuration.Vacancies -> RootComponent.Child.Vacancies(
            DefaultVacanciesComponent(
                componentContext,
                configuration.projectPath.last().id,
                back = { navigation.pop() },
            ),
        )
    }

    private sealed interface Configuration {
        val projectPath: List<ProjectBreadcrumb>
        data class Projects(override val projectPath: List<ProjectBreadcrumb> = emptyList()) :
            Configuration
        data class Dashboard(override val projectPath: List<ProjectBreadcrumb>) : Configuration
        data class Vacancies(override val projectPath: List<ProjectBreadcrumb>) : Configuration
    }
}
