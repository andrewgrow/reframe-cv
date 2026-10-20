package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents the person's identity and professional headline in a resume. */
@Serializable
data class ResumeProfile(
    val name: String = "",
    val headline: String = "",
    val location: String = "",
    val workArrangement: String = "",
    val workAuthorization: String = "",
)
