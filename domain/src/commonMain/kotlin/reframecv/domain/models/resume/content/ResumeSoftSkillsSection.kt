package reframecv.domain.models.resume.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents personal and interpersonal skills in a resume. */
@Serializable
@SerialName("softSkills")
data class ResumeSoftSkillsSection(
    override val title: String = "Soft skills",
    override val visible: Boolean = true,
    val items: List<String> = emptyList(),
) : ResumeSection
