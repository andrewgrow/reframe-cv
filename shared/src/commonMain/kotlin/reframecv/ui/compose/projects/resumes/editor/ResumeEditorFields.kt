package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_headline
import reframecv.shared.generated.resources.resume_keywords
import reframecv.shared.generated.resources.resume_location
import reframecv.shared.generated.resources.resume_name
import reframecv.shared.generated.resources.resume_name_hint
import reframecv.shared.generated.resources.resume_name_required
import reframecv.shared.generated.resources.resume_profile
import reframecv.shared.generated.resources.resume_profile_name
import reframecv.shared.generated.resources.resume_sections
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeEditorFields(
    draft: ResumeDraft,
    onChange: (ResumeDraft) -> Unit,
    enabled: Boolean,
    validationRequested: Boolean,
) {
    val scroll = rememberScrollState()
    val focus = remember { FocusRequester() }
    val form = remember(enabled) { ResumeFormScope(enabled) }
    val nameError = validationRequested && draft.name.isBlank()
    Column(
        Modifier.testTag("resume.fields").verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        // The first field owns focus so Tab enters the form in its visual order.
        Column(Modifier.focusRequester(focus)) {
            form.Field(
                draft.name,
                Res.string.resume_name,
                { onChange(draft.copy(name = it)) },
                tag = "resume.name",
                error = if (nameError) stringResource(Res.string.resume_name_required) else null,
            )
        }
        SelectableText(
            stringResource(Res.string.resume_name_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
        form.Field(draft.keywords, Res.string.resume_keywords, {
            onChange(draft.copy(keywords = it))
        })
        form.Heading(Res.string.resume_profile)
        form.Field(draft.profile.name, Res.string.resume_profile_name, {
            onChange(draft.copy(profile = draft.profile.copy(name = it)))
        })
        form.Field(draft.profile.headline, Res.string.resume_headline, {
            onChange(draft.copy(profile = draft.profile.copy(headline = it)))
        })
        form.Field(draft.profile.location, Res.string.resume_location, {
            onChange(draft.copy(profile = draft.profile.copy(location = it)))
        })
        form.Heading(Res.string.resume_sections)
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
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(nameError) {
        if (nameError) {
            scroll.animateScrollTo(0)
            focus.requestFocus()
        }
    }
}
