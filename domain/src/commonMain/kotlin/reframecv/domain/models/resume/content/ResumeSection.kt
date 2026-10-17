package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Identifies a typed resume section with a custom title and export visibility. */
@Serializable
sealed interface ResumeSection {
    val title: String
    val visible: Boolean
}
