package reframecv.domain.models.resume.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents languages in a resume. */
@Serializable
@SerialName("languages")
data class ResumeLanguagesSection(
    override val title: String = "Languages",
    override val visible: Boolean = true,
    val entries: List<ResumeLanguage> = emptyList(),
) : ResumeSection
