package reframecv.domain.models.resume.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents the professional summary in a resume. */
@Serializable
@SerialName("summary")
data class ResumeSummarySection(
    override val title: String = "Summary",
    override val visible: Boolean = true,
    val text: String = "",
) : ResumeSection
