package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents a language and the user's description of proficiency. */
@Serializable
data class ResumeLanguage(val language: String = "", val level: String = "")
