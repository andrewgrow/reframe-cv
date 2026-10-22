package reframecv.providers.apify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Decodes the stable subset of Apify's run envelope while ignoring billing and engine metadata. */
@Serializable
internal data class ApifyRunResponse(val data: ApifyRunDto)

/** Separates remote run state and output references from our persisted execution model. */
@Serializable
internal data class ApifyRunDto(
    val id: String,
    val status: String,
    val finishedAt: String? = null,
    val defaultDatasetId: String? = null,
    val statusMessage: String? = null,
    val usageTotalUsd: Double? = null,
)

/** Encodes only Actor input fields, independently of local configuration property names. */
@Serializable
internal data class LinkedInJobsInput(
    val urls: List<String>,
    @SerialName("limitPerSource") val limit: Int,
)

/** Decodes optional job fields independently of company and tracking metadata. */
@Serializable
internal data class ApifyVacancyDto(
    val id: JsonPrimitive? = null,
    val title: String? = null,
    val companyName: String? = null,
    val descriptionText: String? = null,
    val descriptionHtml: String? = null,
    val location: String? = null,
    val postedAt: String? = null,
    val salary: JsonElement? = null,
    val employmentType: String? = null,
    val seniorityLevel: String? = null,
)
