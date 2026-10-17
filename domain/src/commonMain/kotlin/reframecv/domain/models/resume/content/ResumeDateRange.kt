package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents incomplete or ongoing work and education periods. */
@Serializable
data class ResumeDateRange(
    val start: ResumeDate? = null,
    val end: ResumeDate? = null,
    val isCurrent: Boolean = false,
) {
    init {
        require(!isCurrent || end == null) { "An ongoing period must not have an end date" }
    }
}
