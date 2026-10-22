package reframecv.providers.apify

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.runners.RemoteRunnerRun
import reframecv.runners.RunnerResultsPage
import reframecv.runners.VacancySearchResult

internal fun ApifyRunDto.toDomain(): RemoteRunnerRun {
    require(id.matches(Regex("[a-zA-Z0-9]+")))
    val state = when (status) {
        "READY" -> ConnectionRunState.Pending
        "RUNNING", "TIMING-OUT", "ABORTING" -> ConnectionRunState.Running
        "SUCCEEDED" -> ConnectionRunState.Succeeded
        "FAILED", "TIMED-OUT" -> ConnectionRunState.Failed
        "ABORTED" -> ConnectionRunState.Aborted
        else -> error("Unsupported Apify status")
    }
    return RemoteRunnerRun(
        id,
        state,
        defaultDatasetId,
        finishedAt?.let { Instant.parse(it).toEpochMilliseconds() },
        statusMessage ?: status,
        usageTotalUsd?.takeIf { it.isFinite() && it >= 0 },
    )
}

internal fun mapApifyPage(
    json: Json,
    body: String,
    offset: Int,
    limit: Int,
    total: Int?,
): RunnerResultsPage {
    val rows = json.parseToJsonElement(body) as? JsonArray ?: error("Expected dataset array")
    val mapped = rows.mapNotNull { row ->
        try {
            json.decodeFromJsonElement<ApifyVacancyDto>(row).toDomain()
        } catch (
            _: IllegalArgumentException,
        ) {
            null
        }
    }.distinctBy { it.externalId }
    val next = offset + rows.size
    val hasMore = rows.isNotEmpty() && (total?.let { next < it } ?: (rows.size >= limit))
    return RunnerResultsPage(
        mapped,
        offset,
        next.takeIf {
            hasMore
        },
        total,
        rows.size - mapped.size,
        rawResults = rows.mapIndexed { index, row ->
            val identifier = ((row as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull
                ?.takeIf { it.matches(Regex("[0-9]+")) }
            RunnerRawResult(offset + index, "LinkedIn", identifier, row.toString())
        },
    )
}

internal fun ApifyVacancyDto.toDomain(): VacancySearchResult? {
    val identifier = id?.contentOrNull?.takeIf { it.matches(Regex("[0-9]+")) }
    val label = title?.let(::cleanText)?.takeIf { it.isNotEmpty() }
    if (identifier == null || label == null) return null
    val text = descriptionText?.takeIf { it.isNotBlank() } ?: htmlText(descriptionHtml.orEmpty())
    val published = postedAt?.let {
        try {
            LocalDate.parse(it).toString()
        } catch (
            _: IllegalArgumentException,
        ) {
            null
        }
    }
    return VacancySearchResult(
        identifier, cleanText(label), companyName?.let(::cleanText).orEmpty(),
        cleanText(text), "https://www.linkedin.com/jobs/view/$identifier",
        location?.let(::cleanText).orEmpty(), published,
        (salary as? JsonPrimitive)?.contentOrNull.orEmpty(),
        employmentType.orEmpty(),
        seniorityLevel.orEmpty(),
    )
}

private fun htmlText(value: String) = value
    .replace(Regex("<(script|style)\\b[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("<br\\s*/?>|</(?:p|div|ul|ol|li|h[1-6])>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<li\\b[^>]*>", RegexOption.IGNORE_CASE), "- ")
    .replace(Regex("<[^>]*>"), "")

private fun cleanText(value: String): String {
    val entities = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"",
        "apos" to "'", "nbsp" to " ", "#39" to "'", "#34" to "\"", "#160" to " ",
    )
    return value.replace(Regex("&(amp|lt|gt|quot|apos|nbsp|#39|#34|#160);")) {
        entities.getValue(it.groupValues[1])
    }.replace("\r\n", "\n").replace("\r", "\n")
        .lines().joinToString("\n") { it.trim() }.replace(Regex("\n{3,}"), "\n\n").trim()
}
