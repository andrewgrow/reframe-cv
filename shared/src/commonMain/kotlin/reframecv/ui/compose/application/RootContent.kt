package reframecv.ui.compose.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import reframecv.ui.components.application.RootComponent
import reframecv.ui.compose.projects.ProjectsContent
import reframecv.ui.compose.projects.dashboard.DashboardContent
import reframecv.ui.compose.projects.vacancies.VacanciesContent

@Composable
fun RootContent(component: RootComponent) {
    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RootNavigation(component.projectTree)
        VerticalDivider()
        Children(
            stack = component.childStack,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) { child ->
            when (val instance = child.instance) {
                is RootComponent.Child.Projects -> ProjectsContent(instance.component)
                is RootComponent.Child.Dashboard -> DashboardContent(instance.component)
                is RootComponent.Child.Vacancies -> VacanciesContent(instance.component)
            }
        }
    }
}
