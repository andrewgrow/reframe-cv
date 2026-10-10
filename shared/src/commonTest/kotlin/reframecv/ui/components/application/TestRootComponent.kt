package reframecv.ui.components.application

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.ui.components.application.navigation.ProjectTreeComponent
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.projects.ProjectsComponent
import reframecv.ui.components.projects.TestProjectsComponent
import reframecv.ui.components.projects.dashboard.DashboardComponent
import reframecv.ui.components.projects.vacancies.VacanciesComponent

class TestRootComponent(
    projectsComponent: ProjectsComponent = TestProjectsComponent(),
    private val onProjectsListClick: () -> Unit = {},
    dashboardComponent: DashboardComponent? = null,
    initialTreeState: ProjectTreeState = ProjectTreeState(loading = false),
    vacanciesComponent: VacanciesComponent? = null,
) : RootComponent {
    override val projectTree = object : ProjectTreeComponent {
        override val state = MutableValue(initialTreeState)
        override fun onToggle(id: Long) = Unit
        override fun onProjectSelected(id: Long) = Unit
        override fun onProjectsList() = onProjectsListClick()
        override fun onRetry() = Unit
    }
    override fun onProjectsList() = onProjectsListClick()
    override val childStack: Value<ChildStack<*, RootComponent.Child>> = MutableValue(
        ChildStack(
            configuration = Unit,
            instance = vacanciesComponent?.let { RootComponent.Child.Vacancies(it) }
                ?: dashboardComponent?.let { RootComponent.Child.Dashboard(it) }
                ?: RootComponent.Child.Projects(projectsComponent),
        ),
    )
}
