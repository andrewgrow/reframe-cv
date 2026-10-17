package reframecv.domain.models.resume.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents education in a resume. */
@Serializable
@SerialName("education")
data class ResumeEducationSection(
    override val title: String = "Education",
    override val visible: Boolean = true,
    val entries: List<ResumeEducation> = emptyList(),
) : ResumeSection
