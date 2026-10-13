package reframecv.ui.components.projects.resumes.editor

import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeProfile
import reframecv.domain.models.resume.content.ResumeSection

/** Keeps unfinished structured resume edits separate from the stored document. */
data class ResumeDraft(
    val name: String = "",
    val keywords: String = "",
    val profile: ResumeProfile = ResumeProfile(),
    val sections: List<ResumeSectionDraft> = emptyList(),
    val nextSectionId: Long = 1,
    val role: ResumeRole = ResumeRole.Regular,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && sections.all { it.isValid }

    fun addSection(section: ResumeSection) = copy(
        sections = sections + ResumeSectionDraft(nextSectionId, section),
        nextSectionId = nextSectionId + 1,
    )

    fun changeSection(section: ResumeSectionDraft) = copy(
        sections = sections.map { if (it.id == section.id) section else it },
    )

    fun removeSection(id: Long) = copy(sections = sections.filterNot { it.id == id })

    fun moveSection(id: Long, offset: Int): ResumeDraft {
        val index = sections.indexOfFirst { it.id == id }
        val destination = index + offset
        if (index < 0 || destination !in sections.indices) return this
        val reordered = sections.toMutableList()
        reordered.add(destination, reordered.removeAt(index))
        return copy(sections = reordered)
    }

    fun toResume(projectId: Long) = applyTo(
        Resume(projectId = projectId, name = "", createdAt = 0, updatedAt = 0),
    )

    fun applyTo(resume: Resume): Resume {
        require(isValid)
        return resume.copy(
            name = name.trim(),
            keywords = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            content = ResumeContent(profile, sections.map { it.toSection() }),
        )
    }

    companion object {
        fun fromResume(resume: Resume, role: ResumeRole = ResumeRole.Regular) = ResumeDraft(
            role = role,
            name = resume.name,
            keywords = resume.keywords.joinToString(", "),
            profile = resume.content.profile,
            sections = resume.content.sections.mapIndexed { index, section ->
                ResumeSectionDraft(index.toLong() + 1, section)
            },
            nextSectionId = resume.content.sections.size.toLong() + 1,
        )
    }
}
