package reframecv.ui.compose.projects.vacancies

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.compose.projects.vacancies.editor.VacancyEditorContent

@Composable
fun VacanciesContent(component: VacanciesComponent) {
    Children(stack = component.childStack) { child ->
        when (val page = child.instance) {
            VacanciesComponent.Child.List -> VacanciesListContent(component)
            is VacanciesComponent.Child.Editor -> VacancyEditorContent(page.component)
        }
    }
}
