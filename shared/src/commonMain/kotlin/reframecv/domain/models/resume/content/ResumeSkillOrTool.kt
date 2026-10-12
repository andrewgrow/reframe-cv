package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents a project skill or tool with a user-defined category. */
@Serializable
data class ResumeSkillOrTool(val name: String = "", val category: String = "")
