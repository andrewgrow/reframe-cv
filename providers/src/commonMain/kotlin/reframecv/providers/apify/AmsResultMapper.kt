package reframecv.providers.apify

import kotlin.time.Instant
import kotlinx.serialization.json.contentOrNull
import reframecv.runners.VacancySearchResult

/** Normalizes AMS snapshots without merging similar postings from different sources. */
internal fun AmsVacancyDto.toDomain(): VacancySearchResult? {
    val identifier = referenceId?.contentOrNull?.takeIf { it.matches(Regex("[0-9]+")) }
    val label = title?.let(::cleanText)?.takeIf { it.isNotBlank() }
    val url = listOf(externalUrl, portalUrl).firstOrNull {
        it != null && Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE).matches(it)
    }
    if (identifier == null || label == null || url == null) return null
    val text = description?.takeIf { it.isNotBlank() } ?: htmlText(descriptionHtml.orEmpty())
    val published = publishedDate?.let {
        try {
            Instant.parse(it).toString().substringBefore('T')
        } catch (_: IllegalArgumentException) {
            null
        }
    }
    return VacancySearchResult(
        externalId = identifier,
        title = label,
        company = employer?.let(::cleanText).orEmpty(),
        description = cleanText(text),
        url = url,
        location = location?.let(::cleanText).orEmpty(),
        postedAt = published,
        salary = salary.orEmpty(),
        employmentType = contractType.orEmpty(),
        source = "AMS",
    )
}
