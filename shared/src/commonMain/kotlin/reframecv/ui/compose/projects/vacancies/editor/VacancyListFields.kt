package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_list_hint
import reframecv.shared.generated.resources.vacancy_nice_to_have
import reframecv.shared.generated.resources.vacancy_requirements
import reframecv.shared.generated.resources.vacancy_resize_list
import reframecv.shared.generated.resources.vacancy_responsibilities
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.compose.common.ResizableTextField
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyListFields(
    draft: VacancyDraft,
    onChange: (VacancyDraft) -> Unit,
    enabled: Boolean,
    scroll: ScrollState,
    viewport: Rect,
) {
    VacancyListField(
        draft.responsibilities,
        stringResource(Res.string.vacancy_responsibilities),
        "vacancy.responsibilities",
        enabled,
        scroll,
        viewport,
    ) { onChange(draft.copy(responsibilities = it)) }
    VacancyListField(
        draft.requirements,
        stringResource(Res.string.vacancy_requirements),
        "vacancy.requirements",
        enabled,
        scroll,
        viewport,
    ) { onChange(draft.copy(requirements = it)) }
    VacancyListField(
        draft.niceToHave,
        stringResource(Res.string.vacancy_nice_to_have),
        "vacancy.niceToHave",
        enabled,
        scroll,
        viewport,
    ) { onChange(draft.copy(niceToHave = it)) }
}

@Composable
private fun VacancyListField(
    items: List<String>,
    label: String,
    tag: String,
    enabled: Boolean,
    scroll: ScrollState,
    viewport: Rect,
    onChange: (List<String>) -> Unit,
) {
    ResizableTextField(
        value = items.joinToString("\n"),
        onValueChange = { onChange(it.lines()) },
        enabled = enabled,
        scroll = scroll,
        viewport = viewport,
        label = label,
        resizeLabel = stringResource(Res.string.vacancy_resize_list, label),
        tag = tag,
        minimumHeight = ReframeTheme.tokens.dimensions.vacancyDescriptionMinHeight,
        initialHeight = ReframeTheme.tokens.dimensions.vacancyDescriptionDefaultHeight,
    )
    SelectableText(
        stringResource(Res.string.vacancy_list_hint),
        color = ReframeTheme.colorScheme.hint,
        style = ReframeTheme.tokens.typography.bodySmall,
    )
}
