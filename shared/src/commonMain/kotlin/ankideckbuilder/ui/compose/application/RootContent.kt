package ankideckbuilder.ui.compose.application

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import ankideckbuilder.ui.components.application.RootComponent
import ankideckbuilder.ui.compose.projects.ProjectsContent
import com.arkivanov.decompose.extensions.compose.stack.Children

@Composable
fun RootContent(component: RootComponent) {
    MaterialTheme {
        Children(stack = component.childStack) { child ->
            when (val instance = child.instance) {
                is RootComponent.Child.Projects -> ProjectsContent(instance.component)
            }
        }
    }
}
