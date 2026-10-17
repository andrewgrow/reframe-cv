package reframecv.domain.models.resume.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents grouped skills in a resume. */
@Serializable
@SerialName("skills")
data class ResumeSkillsSection(
    override val title: String = "Skills",
    override val visible: Boolean = true,
    val groups: List<ResumeSkillGroup> = emptyList(),
) : ResumeSection
