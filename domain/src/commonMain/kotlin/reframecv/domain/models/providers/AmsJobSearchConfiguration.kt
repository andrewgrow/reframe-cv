package reframecv.domain.models.providers

import kotlinx.serialization.Serializable

/** Configures public AMS searches including listings indexed from external job boards. */
@Serializable
data class AmsJobSearchConfiguration(
    override val actorId: String = "blackfalcondata/ams-austria-job-scraper",
    val query: String = "",
    val location: String = "",
    val radiusKm: Int = 50,
    override val maxResults: Int = 50,
    val includeExternalJobs: Boolean = true,
    val postedWithin: String = "",
    val version: Int = 1,
) : RunnerConfiguration {
    override val valid: Boolean get() = validIdentity && validSearch &&
        maxResults in 1..10000 && radiusKm in setOf(0, 5, 10, 20, 50, 75, 100) &&
        postedWithin in setOf("", "LAST_WEEK", "LAST_MONTH")
    private val validIdentity get() = version == 1 &&
        actorId == "blackfalcondata/ams-austria-job-scraper"
    private val validSearch get() = query.isNotBlank() &&
        (radiusKm == 0 || location.isNotBlank())
}
