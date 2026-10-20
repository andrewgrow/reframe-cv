package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents a job, its achievements, and professional projects. */
@Serializable
data class ResumeExperience(
    val position: String = "",
    val company: String = "",
    val location: String = "",
    val period: ResumeDateRange = ResumeDateRange(),
    val achievements: List<String> = emptyList(),
    val projects: List<ResumeProject> = emptyList(),
    val description: String = "",
    val links: List<ResumeLink> = emptyList(),
)
