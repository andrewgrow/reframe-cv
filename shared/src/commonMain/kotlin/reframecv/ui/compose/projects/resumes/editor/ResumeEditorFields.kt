package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_sections
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumePhotoComponent
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeEditorFields(
    draft: ResumeDraft,
    onChange: (ResumeDraft) -> Unit,
    enabled: Boolean,
    validationRequested: Boolean,
    photoComponent: ResumePhotoComponent,
) {
    val scroll = rememberScrollState()
    val focus = remember { FocusRequester() }
    val form = remember(enabled) { ResumeFormScope(enabled) }
    val nameError = validationRequested && draft.name.isBlank()
    val tokens = ReframeTheme.tokens
    FastScrollColumn(
        scrollState = scroll,
        contentModifier = Modifier.testTag("resume.fields"),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        form.NameAndPhotoFields(draft, onChange, focus, nameError, photoComponent)
        form.MetadataFields(draft, onChange)
        Column(
            Modifier.padding(top = tokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        ) {
            form.ProfileFields(draft.profile) { onChange(draft.copy(profile = it)) }
        }
        Box(Modifier.padding(top = tokens.spacing.medium)) {
            form.Heading(Res.string.resume_sections)
        }
        draft.sections.forEachIndexed { index, section ->
            key(section.id) {
                ResumeSectionEditor(
                    section,
                    form,
                    { onChange(draft.changeSection(it)) },
                    onRemove = { onChange(draft.removeSection(section.id)) },
                    onMove = { onChange(draft.moveSection(section.id, it)) },
                    first = index == 0,
                    last = index == draft.sections.lastIndex,
                )
            }
        }
        ResumeSectionPicker(enabled) { onChange(draft.addSection(it)) }
        form.ConsentFields(draft, onChange)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(nameError) {
        if (nameError) {
            scroll.animateScrollTo(0)
            focus.requestFocus()
        }
    }
}
