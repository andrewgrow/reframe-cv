package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents a professional project described within work experience. */
@Serializable
data class ResumeProject(
    val name: String = "",
    val description: String = "",
    val skillsOrTools: List<ResumeSkillOrTool> = emptyList(),
    val links: List<ResumeLink> = emptyList(),
)
