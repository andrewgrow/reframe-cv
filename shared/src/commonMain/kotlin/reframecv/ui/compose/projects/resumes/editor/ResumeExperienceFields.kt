package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import reframecv.domain.models.resume.content.ResumeExperience
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeProject
import reframecv.domain.models.resume.content.ResumeSkillOrTool
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_achievements
import reframecv.shared.generated.resources.resume_add_entry
import reframecv.shared.generated.resources.resume_add_project
import reframecv.shared.generated.resources.resume_add_tool
import reframecv.shared.generated.resources.resume_category
import reframecv.shared.generated.resources.resume_company
import reframecv.shared.generated.resources.resume_description
import reframecv.shared.generated.resources.resume_location
import reframecv.shared.generated.resources.resume_position
import reframecv.shared.generated.resources.resume_project_name
import reframecv.shared.generated.resources.resume_projects
import reframecv.shared.generated.resources.resume_remove
import reframecv.shared.generated.resources.resume_tool_name
import reframecv.shared.generated.resources.resume_tools
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
            Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small)) {
                ExperienceIdentity(entry, change)
                PeriodFields(draft.periods[index]) { period ->
                    onChange(
                        draft.copy(
                            periods = draft.periods.replacedAt(index, period),
                        ),
                    )
                }
                ExperienceDetails(entry, change)
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
                value = section.copy(entries = section.entries + ResumeExperience()),
                periods = draft.periods + ResumePeriodDraft(),
            ),
        )
    })
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
    Heading(Res.string.resume_projects)
    Entries(entry.projects, {
        change(entry.copy(projects = it))
    }, ::ResumeProject, Res.string.resume_add_project) { project, update ->
        Field(project.name, Res.string.resume_project_name, { update(project.copy(name = it)) })
        Field(project.description, Res.string.resume_description, {
            update(project.copy(description = it))
        }, multiline = true)
        Heading(Res.string.resume_tools)
        Entries(project.skillsOrTools, {
            update(project.copy(skillsOrTools = it))
        }, ::ResumeSkillOrTool, Res.string.resume_add_tool) { tool, edit ->
            Field(tool.name, Res.string.resume_tool_name, { edit(tool.copy(name = it)) })
            Field(tool.category, Res.string.resume_category, { edit(tool.copy(category = it)) })
        }
    }
}
