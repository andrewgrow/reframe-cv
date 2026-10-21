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
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.ui.components.application.navigation.DefaultProjectTreeComponent
import reframecv.ui.components.application.navigation.ProjectTreeComponent
import reframecv.ui.components.common.ai.AiBalanceNotice
import reframecv.ui.components.common.section.CollapsibleSectionComponent
import reframecv.ui.components.common.section.DefaultCollapsibleSectionComponent
import reframecv.ui.components.projects.DefaultProjectsComponent
import reframecv.ui.components.projects.ProjectPathEntry
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.coverletters.CoverLettersComponent
import reframecv.ui.components.projects.coverletters.DefaultCoverLettersComponent
import reframecv.ui.components.projects.dashboard.DashboardComponent
import reframecv.ui.components.projects.dashboard.DefaultDashboardComponent
import reframecv.ui.components.projects.resumes.DefaultResumesComponent
import reframecv.ui.components.projects.resumes.ResumesComponent
import reframecv.ui.components.projects.vacancies.DefaultVacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.components.settings.DefaultSettingsComponent
import reframecv.ui.components.settings.SettingsComponent
import reframecv.ui.context.AppComponentContext

/** Lets the application shell show the selected screen alongside project navigation. */
interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>
    val balanceNotice: Value<AiBalanceNotice>
    val navigationPanel: CollapsibleSectionComponent
    val projectTree: ProjectTreeComponent
    fun onSettings()
    fun onProjectsList()

    /** Lets the application shell render the component belonging to the active destination. */
    sealed interface Child {
        /** Lets the root render global settings. */
        data class Settings(val component: SettingsComponent) : Child

        /** Lets the root render project organization screens. */
        data class Projects(val component: ProjectsComponent) : Child

        /** Lets the root render the workspace overview. */
        data class Dashboard(val component: DashboardComponent) : Child

        /** Lets the root render the selected project's resumes. */
        data class Resumes(val component: ResumesComponent) : Child

        /** Lets the root render the selected project's cover letters. */
        data class CoverLetters(val component: CoverLettersComponent) : Child

        /** Lets the root render the selected project's vacancies. */
        data class Vacancies(val component: VacanciesComponent) : Child
    }
}

/** Lets users open projects and return to their parents from any entry point. */
class DefaultRootComponent(componentContext: AppComponentContext) :
    RootComponent,
    AppComponentContext by componentContext {
    override val balanceNotice get() = dependencies.aiRequestBalance.state
    override val navigationPanel = DefaultCollapsibleSectionComponent(
        componentContext,
        "navigation_panel",
        ScreenStateOwner(ScreenType.Application),
    )
    private val navigation = StackNavigation<Configuration>()
    private var activeProjects: List<Project> = emptyList()

    override fun onProjectsList() {
        if (isDocumentEditorBusy()) return
        navigation.popTo(0)
    }

    override fun onSettings() {
        if (isDocumentEditorBusy()) return
        val existing = childStack.value.items.indexOfLast {
            it.configuration == Configuration.Settings
        }
        if (existing >=
            0
        ) {
            navigation.popTo(existing)
        } else {
            navigation.pushNew(Configuration.Settings)
        }
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
        openProject = { projects ->
            if (!isDocumentEditorBusy()) openPath(projects.map { ProjectPathEntry(it.id, it.name) })
        },
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

    private fun isDocumentEditorBusy(): Boolean = when (
        val child = childStack.value.active.instance
    ) {
        is RootComponent.Child.Settings -> child.component.isBusy
        is RootComponent.Child.Vacancies -> child.component.isBusy
        is RootComponent.Child.Resumes -> child.component.isBusy
        is RootComponent.Child.CoverLetters -> child.component.isBusy
        else -> false
    }

    private fun openPath(
        path: List<ProjectPathEntry>,
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

    private fun openParentList(path: List<ProjectPathEntry>) {
        if (path.isEmpty()) return
        val parentPath = path.dropLast(1)
        navigation.navigate(
            transformer = {
                listOf(Configuration.Projects()) + parentPath.indices.map { index ->
                    Configuration.Projects(parentPath.take(index + 1))
                }
            },
            onComplete = { _, _ -> },
        )
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
        Configuration.Settings -> RootComponent.Child.Settings(
            DefaultSettingsComponent(componentContext, back = { navigation.pop() }),
        )

        is Configuration.Projects -> RootComponent.Child.Projects(
            DefaultProjectsComponent(
                componentContext,
                projectPath = configuration.projectPath,
                back = { openParentList(configuration.projectPath) },
                onProjectConfigured = {
                    navigation.pushNew(Configuration.Dashboard(configuration.projectPath))
                },
                onProjectOpened = { project ->
                    openPath(
                        configuration.projectPath + ProjectPathEntry(project.id, project.name),
                        project.mode,
                    )
                },
            ),
        )

        is Configuration.Dashboard -> RootComponent.Child.Dashboard(
            DefaultDashboardComponent(
                componentContext,
                configuration.projectPath.last().id,
                back = { openParentList(configuration.projectPath) },
                openCoverLetters = {
                    navigation.pushNew(Configuration.CoverLetters(configuration.projectPath))
                },
                openResumes = {
                    navigation.pushNew(Configuration.Resumes(configuration.projectPath))
                },
                openVacancies = {
                    navigation.pushNew(Configuration.Vacancies(configuration.projectPath))
                },
            ),
        )

        is Configuration.CoverLetters -> RootComponent.Child.CoverLetters(
            DefaultCoverLettersComponent(
                componentContext,
                configuration.projectPath.last().id,
                back = { navigation.pop() },
            ),
        )

        is Configuration.Resumes -> RootComponent.Child.Resumes(
            DefaultResumesComponent(
                componentContext,
                configuration.projectPath.last().id,
                back = { navigation.pop() },
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

    /** Keeps each destination tied to its project so Back can reach the parent. */
    private sealed interface Configuration {
        val projectPath: List<ProjectPathEntry>

        /** Identifies application-wide preferences independently of a project. */
        data object Settings : Configuration {
            override val projectPath: List<ProjectPathEntry> = emptyList()
        }

        /** Identifies which project's children the destination should show. */
        data class Projects(override val projectPath: List<ProjectPathEntry> = emptyList()) :
            Configuration

        /** Identifies which workspace the destination should show. */
        data class Dashboard(override val projectPath: List<ProjectPathEntry>) : Configuration

        /** Identifies which project's resumes the destination should show. */
        data class Resumes(override val projectPath: List<ProjectPathEntry>) : Configuration

        /** Identifies which project's cover letters the destination should show. */
        data class CoverLetters(override val projectPath: List<ProjectPathEntry>) : Configuration

        /** Identifies which project's vacancies the destination should show. */
        data class Vacancies(override val projectPath: List<ProjectPathEntry>) : Configuration
    }
}
