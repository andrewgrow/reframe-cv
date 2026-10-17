package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents a resume's consent statement and whether to include it in the document. */
@Serializable
data class ResumeConsent(val text: String = "", val visible: Boolean = true)
