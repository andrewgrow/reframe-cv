package reframecv.domain.models.resume.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents a custom text section in a resume. */
@Serializable
@SerialName("text")
data class ResumeTextSection(
    override val title: String = "",
    override val visible: Boolean = true,
    val text: String = "",
) : ResumeSection
