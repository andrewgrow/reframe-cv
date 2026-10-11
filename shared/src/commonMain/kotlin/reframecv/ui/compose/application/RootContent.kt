package reframecv.ui.compose.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import reframecv.ui.components.application.RootComponent
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.compose.projects.ProjectsContent
import reframecv.ui.compose.projects.dashboard.DashboardContent
import reframecv.ui.compose.projects.vacancies.VacanciesContent

@Composable
fun RootContent(component: RootComponent) {
    val stack by component.childStack.subscribeAsState()
    val active = stack.active.instance
    val editorOpen = when (active) {
        is RootComponent.Child.Vacancies -> {
            val editor by active.component.editorSlot.subscribeAsState()
            editor.child != null
        }

        is RootComponent.Child.Projects -> {
            val editor by active.component.editorSlot.subscribeAsState()
            editor.child != null
        }

        is RootComponent.Child.Dashboard -> false
    }
    Row(
        Modifier.testTag(
            "app.root",
        ).fillMaxSize().background(MaterialTheme.colorScheme.background).pageBackHandler(
            enabled = (
                active !is RootComponent.Child.Projects ||
                    active.component.projectPath.isNotEmpty()
                ) && !editorOpen,
            destination = active,
            onBack = {
                when (active) {
                    is RootComponent.Child.Dashboard -> active.component.onBack()
                    is RootComponent.Child.Vacancies -> active.component.onBack()
                    is RootComponent.Child.Projects -> active.component.onBack()
                }
            },
        ),
    ) {
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
