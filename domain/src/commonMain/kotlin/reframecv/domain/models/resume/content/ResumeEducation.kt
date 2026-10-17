package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents an education entry with its qualification and study period. */
@Serializable
data class ResumeEducation(
    val institution: String = "",
    val degree: String = "",
    val specialty: String = "",
    val period: ResumeDateRange = ResumeDateRange(),
)
