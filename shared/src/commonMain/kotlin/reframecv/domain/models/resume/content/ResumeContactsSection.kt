package reframecv.domain.models.resume.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents contact methods in a resume. */
@Serializable
@SerialName("contacts")
data class ResumeContactsSection(
    override val title: String = "Contacts",
    override val visible: Boolean = true,
    val entries: List<ResumeContact> = emptyList(),
) : ResumeSection
