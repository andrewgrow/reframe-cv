package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.resume.content.ResumeContactsSection
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeLanguagesSection
import reframecv.domain.models.resume.content.ResumeSection
import reframecv.domain.models.resume.content.ResumeSkillsSection
import reframecv.domain.models.resume.content.ResumeSoftSkillsSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.ResumeTextSection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_section_title
import reframecv.shared.generated.resources.resume_summary
import reframecv.ui.components.projects.resumes.editor.ResumeSectionDraft
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeSectionEditor(
    draft: ResumeSectionDraft,
    form: ResumeFormScope,
    onChange: (ResumeSectionDraft) -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
    first: Boolean,
    last: Boolean,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var confirmingRemoval by rememberSaveable { mutableStateOf(false) }
    val tokens = ReframeTheme.tokens
    Surface(
        modifier = Modifier.testTag("resume.section.${draft.id}"),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
        border = BorderStroke(
            tokens.outlineWidth,
            if (draft.isValid) ReframeTheme.colorScheme.hint else ReframeTheme.colorScheme.critical,
        ),
    ) {
        Column(
            Modifier.padding(tokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            if (draft.value is ResumeSummarySection) {
                form.Heading(Res.string.resume_summary)
            } else {
                form.Field(draft.value.title, Res.string.resume_section_title, {
                    onChange(draft.copy(value = draft.value.withMetadata(title = it)))
                })
            }
            form.SectionActions(
                removeLabel = draft.value.removeLabel(),
                expanded = expanded,
                visible = draft.value.visible,
                first = first,
                last = last,
                onExpand = { expanded = !expanded },
                onVisibilityChange = {
                    onChange(draft.copy(value = draft.value.withMetadata(visible = it)))
                },
                onMove = onMove,
                onRemove = { if (form.enabled) confirmingRemoval = true },
            )
            AnimatedVisibility(expanded) { form.SectionBody(draft, onChange) }
        }
    }
    if (confirmingRemoval) {
        val name = if (draft.value is ResumeSummarySection) {
            stringResource(Res.string.resume_summary)
        } else {
            draft.value.title
        }
        ResumeSectionRemoveConfirmation(
            name,
            removeLabel = draft.value.removeLabel(),
            enabled = form.enabled,
            onCancel = { confirmingRemoval = false },
            onConfirm = {
                if (form.enabled) {
                    confirmingRemoval = false
                    onRemove()
                }
            },
        )
    }
}

@Composable
private fun ResumeFormScope.SectionBody(
    draft: ResumeSectionDraft,
    onChange: (ResumeSectionDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
        when (draft.value) {
            is ResumeExperienceSection -> ExperienceFields(draft, onChange)
            is ResumeEducationSection -> EducationFields(draft, onChange)
            else -> SimpleSectionFields(draft.value) { onChange(draft.copy(value = it)) }
        }
    }
}

private fun ResumeSection.withMetadata(
    title: String = this.title,
    visible: Boolean = this.visible,
): ResumeSection = when (this) {
    is ResumeSummarySection -> copy(title = title, visible = visible)
    is ResumeSkillsSection -> copy(title = title, visible = visible)
    is ResumeExperienceSection -> copy(title = title, visible = visible)
    is ResumeEducationSection -> copy(title = title, visible = visible)
    is ResumeLanguagesSection -> copy(title = title, visible = visible)
    is ResumeSoftSkillsSection -> copy(title = title, visible = visible)
    is ResumeContactsSection -> copy(title = title, visible = visible)
    is ResumeTextSection -> copy(title = title, visible = visible)
}
