package reframecv.ui.components.projects.resumes.editor

import reframecv.domain.models.resume.content.ResumeDate
import reframecv.domain.models.resume.content.ResumeDateRange

/** Keeps partially typed dates editable without constructing invalid domain dates. */
data class ResumePeriodDraft(
    val start: String = "",
    val end: String = "",
    val isCurrent: Boolean = false,
) {
    val isValid: Boolean
        get() = validDate(start) && (isCurrent || validDate(end))

    fun toPeriod(): ResumeDateRange {
        require(isValid)
        return ResumeDateRange(parseDate(start), if (isCurrent) null else parseDate(end), isCurrent)
    }

    companion object {
        private const val MAX_YEAR = 9999
        private const val MAX_MONTH = 12
        private val datePattern = Regex("[0-9]{4}(-[0-9]{2})?")

        fun validDate(value: String): Boolean {
            if (value.isBlank()) return true
            val date = value.trim()
            return if (datePattern.matches(date)) {
                val parts = date.split('-')
                parts.first().toInt() in 1..MAX_YEAR &&
                    (parts.size == 1 || parts.last().toInt() in 1..MAX_MONTH)
            } else {
                false
            }
        }

        fun fromPeriod(period: ResumeDateRange) = ResumePeriodDraft(
            formatDate(period.start),
            formatDate(period.end),
            period.isCurrent,
        )

        private fun formatDate(date: ResumeDate?): String = date?.let {
            val year = it.year.toString().padStart(4, '0')
            it.month?.let { month -> "$year-${month.toString().padStart(2, '0')}" } ?: year
        }.orEmpty()

        private fun parseDate(value: String): ResumeDate? {
            if (value.isBlank()) return null
            val parts = value.trim().split('-')
            return ResumeDate(parts.first().toInt(), parts.getOrNull(1)?.toInt())
        }
    }
}
