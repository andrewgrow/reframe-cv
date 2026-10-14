package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import reframecv.domain.models.resume.content.ResumeExperience
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_achievements
import reframecv.shared.generated.resources.resume_add_position
import reframecv.shared.generated.resources.resume_company
import reframecv.shared.generated.resources.resume_location
import reframecv.shared.generated.resources.resume_position
import reframecv.shared.generated.resources.resume_projects
import reframecv.ui.components.projects.resumes.editor.ResumePeriodDraft
import reframecv.ui.components.projects.resumes.editor.ResumeSectionDraft
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.ExperienceFields(
    draft: ResumeSectionDraft,
    onChange: (ResumeSectionDraft) -> Unit,
) {
    val section = draft.value as ResumeExperienceSection
    section.entries.forEachIndexed { index, entry ->
        key(index, section.entries.size) {
            val change: (ResumeExperience) -> Unit = { replacement ->
                onChange(
                    draft.copy(
                        value = section.copy(
                            entries = section.entries.replacedAt(index, replacement),
                        ),
                    ),
                )
            }
            Column(
                Modifier.padding(vertical = ReframeTheme.tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
            ) {
                ExperienceIdentity(entry, change)
                PeriodFields(draft.periods[index]) { period ->
                    onChange(
                        draft.copy(
                            periods = draft.periods.replacedAt(index, period),
                        ),
                    )
                }
                ExperienceDetails(entry, change)
                PositionRemoveAction(draft, index, onChange)
            }
        }
    }
    Box(Modifier.padding(top = ReframeTheme.tokens.spacing.medium)) {
        Action(Res.string.resume_add_position, {
            onChange(
                draft.copy(
                    value = section.copy(entries = section.entries + ResumeExperience()),
                    periods = draft.periods + ResumePeriodDraft(),
                ),
            )
        })
    }
}

@Composable
private fun ResumeFormScope.ExperienceIdentity(
    entry: ResumeExperience,
    change: (ResumeExperience) -> Unit,
) {
    Field(entry.position, Res.string.resume_position, { change(entry.copy(position = it)) })
    Field(entry.company, Res.string.resume_company, { change(entry.copy(company = it)) })
    Field(entry.location, Res.string.resume_location, { change(entry.copy(location = it)) })
}

@Composable
private fun ResumeFormScope.ExperienceDetails(
    entry: ResumeExperience,
    change: (ResumeExperience) -> Unit,
) {
    Field(entry.achievements.joinToString("\n"), Res.string.resume_achievements, {
        change(entry.copy(achievements = it.split('\n')))
    }, multiline = true)
    Column(
        Modifier.padding(top = ReframeTheme.tokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.large),
    ) {
        Heading(Res.string.resume_projects)
        ProjectEntries(entry.projects) { change(entry.copy(projects = it)) }
    }
}
