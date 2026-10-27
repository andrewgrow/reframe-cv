package reframecv.providers.apify

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** Requests full plain-text AMS snapshots without remote incremental filtering or truncation. */
@Serializable
internal data class AmsJobsInput(
    val query: String,
    val location: String,
    val radiusKm: Int,
    val maxResults: Int,
    val includeExternalJobs: Boolean,
    val postedWithin: String,
    val includeDetails: Boolean = true,
    val sortBy: String = "date",
    val mode: String = "full",
    val compact: Boolean = false,
    val descriptionFormat: String = "text",
    val descriptionMaxLength: Int = 0,
)

/** Reads importable fields while the full, untouched dataset row remains in local history. */
@Serializable
internal data class AmsVacancyDto(
    val referenceId: JsonPrimitive? = null,
    val title: String? = null,
    val employer: String? = null,
    val description: String? = null,
    val descriptionHtml: String? = null,
    val externalUrl: String? = null,
    val portalUrl: String? = null,
    val location: String? = null,
    val publishedDate: String? = null,
    val salary: String? = null,
    val contractType: String? = null,
)
