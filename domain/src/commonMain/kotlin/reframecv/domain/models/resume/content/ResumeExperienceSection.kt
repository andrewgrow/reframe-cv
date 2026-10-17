package reframecv.domain.models.resume.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents work experience in a resume. */
@Serializable
@SerialName("experience")
data class ResumeExperienceSection(
    override val title: String = "Work experience",
    override val visible: Boolean = true,
    val entries: List<ResumeExperience> = emptyList(),
) : ResumeSection
