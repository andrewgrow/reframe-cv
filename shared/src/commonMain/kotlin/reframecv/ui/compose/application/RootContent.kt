package reframecv.ui.compose.application

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import reframecv.ui.components.application.RootComponent
import reframecv.ui.compose.projects.ProjectsContent

@Composable
fun RootContent(component: RootComponent) {
    Children(stack = component.childStack) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Projects -> ProjectsContent(instance.component)
        }
    }
}
