package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import reframecv.domain.models.resume.content.ResumeEducation
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_add_entry
import reframecv.shared.generated.resources.resume_degree
import reframecv.shared.generated.resources.resume_institution
import reframecv.shared.generated.resources.resume_remove
import reframecv.shared.generated.resources.resume_specialty
import reframecv.ui.components.projects.resumes.editor.ResumePeriodDraft
import reframecv.ui.components.projects.resumes.editor.ResumeSectionDraft
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.EducationFields(
    draft: ResumeSectionDraft,
    onChange: (ResumeSectionDraft) -> Unit,
) {
    val section = draft.value as ResumeEducationSection
    section.entries.forEachIndexed { index, entry ->
        key(index, section.entries.size) {
            val change: (ResumeEducation) -> Unit = { replacement ->
                onChange(
                    draft.copy(
                        value = section.copy(
                            entries = section.entries.replacedAt(index, replacement),
                        ),
                    ),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
                Field(entry.institution, Res.string.resume_institution, {
                    change(entry.copy(institution = it))
                })
                Field(entry.degree, Res.string.resume_degree, { change(entry.copy(degree = it)) })
                Field(entry.specialty, Res.string.resume_specialty, {
                    change(entry.copy(specialty = it))
                })
                PeriodFields(draft.periods[index]) { period ->
                    onChange(
                        draft.copy(
                            periods = draft.periods.replacedAt(index, period),
                        ),
                    )
                }
                Action(Res.string.resume_remove, {
                    onChange(
                        draft.copy(
                            value = section.copy(
                                entries = section.entries.removedAt(index),
                            ),
                            periods = draft.periods.removedAt(index),
                        ),
                    )
                })
            }
        }
    }
    Action(Res.string.resume_add_entry, {
        onChange(
            draft.copy(
                value = section.copy(entries = section.entries + ResumeEducation()),
                periods = draft.periods + ResumePeriodDraft(),
            ),
        )
    })
}
