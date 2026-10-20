package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents a labeled application or project URL within professional experience. */
@Serializable
data class ResumeLink(val label: String = "", val url: String = "")
