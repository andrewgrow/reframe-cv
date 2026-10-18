package reframecv.domain.models.vacancy

private const val MAX_ATS_SCORE = 100

/** Links a resume to a vacancy with its vacancy-specific ATS match score. */
data class VacancyResume(val resumeId: Long, val atsScore: Int = 0) {
    init {
        require(atsScore in 0..MAX_ATS_SCORE) { "ATS score must be between 0 and 100" }
    }
}
