package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents editable resume data with explicitly ordered sections. */
@Serializable
data class ResumeContent(
    val profile: ResumeProfile = ResumeProfile(),
    val sections: List<ResumeSection> = emptyList(),
    val language: String = "",
    val consent: ResumeConsent = ResumeConsent(),
)
