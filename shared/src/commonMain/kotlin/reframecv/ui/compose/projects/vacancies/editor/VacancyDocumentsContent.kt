package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_list_title
import reframecv.shared.generated.resources.resume_list_title
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.compose.common.CollapsibleSection
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyDocumentsContent(component: VacancyEditorComponent, enabled: Boolean) {
    val draft by component.draft.subscribeAsState()
    val resumes by component.resumes.state.subscribeAsState()
    val resumeSection by component.resumeSection.state.subscribeAsState()
    val letters by component.letterSection.state.subscribeAsState()
    CollapsibleSection(
        stringResource(Res.string.resume_list_title, draft.resumes.size),
        resumeSection,
        component.resumeSection::onToggle,
        "vacancy.resume_section",
    ) {
        VacancyResumesContent(
            draft.resumes,
            resumes,
            component.resumes,
            enabled,
            component.assessment,
            component.improvement,
            showTitle = false,
        )
    }
    Spacer(Modifier.height(ReframeTheme.tokens.spacing.large))
    CollapsibleSection(
        stringResource(Res.string.letter_list_title, draft.coverLetterIds.size),
        letters,
        component.letterSection::onToggle,
        "vacancy.letter_section",
    ) {
        VacancyLettersContent(component, enabled)
    }
}
