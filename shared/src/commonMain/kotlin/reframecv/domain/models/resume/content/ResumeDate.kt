package reframecv.domain.models.resume.content

import kotlinx.serialization.Serializable

/** Represents resume dates known to a year or to a year and month. */
@Serializable
data class ResumeDate(val year: Int, val month: Int? = null) {
    init {
        require(year in 1..MAX_YEAR) { "Year must be between 1 and 9999" }
        require(month == null || month in 1..MONTHS_PER_YEAR) { "Month must be between 1 and 12" }
    }

    private companion object {
        const val MAX_YEAR = 9999
        const val MONTHS_PER_YEAR = 12
    }
}
