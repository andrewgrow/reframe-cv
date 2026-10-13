package reframecv.ui.components.projects.resumes.editor

import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeSection

/** Gives sections stable editor identities and preserves unfinished work/education dates. */
data class ResumeSectionDraft(
    val id: Long,
    val value: ResumeSection,
    val periods: List<ResumePeriodDraft> = when (value) {
        is ResumeExperienceSection -> value.entries.map { ResumePeriodDraft.fromPeriod(it.period) }
        is ResumeEducationSection -> value.entries.map { ResumePeriodDraft.fromPeriod(it.period) }
        else -> emptyList()
    },
) {
    val isValid: Boolean
        get() = periods.all { it.isValid } && when (value) {
            is ResumeExperienceSection -> periods.size == value.entries.size
            is ResumeEducationSection -> periods.size == value.entries.size
            else -> periods.isEmpty()
        }

    fun toSection(): ResumeSection = when (val section = value) {
        is ResumeExperienceSection -> section.copy(
            entries = section.entries.mapIndexed { index, entry ->
                entry.copy(period = periods[index].toPeriod())
            },
        )

        is ResumeEducationSection -> section.copy(
            entries = section.entries.mapIndexed { index, entry ->
                entry.copy(period = periods[index].toPeriod())
            },
        )

        else -> section
    }
}
