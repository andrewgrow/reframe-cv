package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents a contact method with a custom display label. */
@Serializable
data class ResumeContact(val type: String = "", val label: String = "", val value: String = "")
