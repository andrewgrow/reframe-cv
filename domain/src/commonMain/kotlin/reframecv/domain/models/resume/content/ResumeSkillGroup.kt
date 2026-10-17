package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Groups skills under a profession-specific category chosen by the user. */
@Serializable
data class ResumeSkillGroup(val title: String = "", val items: List<String> = emptyList())
