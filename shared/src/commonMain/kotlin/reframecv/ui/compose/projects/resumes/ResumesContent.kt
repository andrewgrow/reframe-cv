package reframecv.ui.compose.projects.resumes

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import reframecv.ui.components.projects.resumes.ResumesComponent
import reframecv.ui.compose.projects.resumes.editor.ResumeEditorContent

@Composable
fun ResumesContent(component: ResumesComponent) {
    Children(stack = component.childStack) { child ->
        when (val page = child.instance) {
            ResumesComponent.Child.List -> ResumesListContent(component)
            is ResumesComponent.Child.Editor -> ResumeEditorContent(page.component)
        }
    }
}
