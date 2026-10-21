package reframecv.ui.compose.projects.coverletters

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import reframecv.ui.components.projects.coverletters.CoverLettersComponent
import reframecv.ui.compose.projects.vacancies.VacanciesContent

@Composable
fun CoverLettersContent(component: CoverLettersComponent) {
    Children(stack = component.childStack) { child ->
        when (val page = child.instance) {
            CoverLettersComponent.Child.List -> CoverLettersListContent(component)
            is CoverLettersComponent.Child.Editor -> CoverLetterEditorContent(page.component)
            is CoverLettersComponent.Child.LinkedVacancy -> VacanciesContent(page.component)
        }
    }
}
