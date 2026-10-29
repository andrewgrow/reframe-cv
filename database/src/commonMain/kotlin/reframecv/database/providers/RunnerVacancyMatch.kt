package reframecv.database.providers

import androidx.room3.ColumnInfo
import androidx.room3.Query
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import reframecv.runners.SourcePostingIdentity
import reframecv.runners.postingUrlIdentity

/** Selects only fields denoting a posting, excluding company, application and search-page URLs. */
private const val POSTING_FIELDS =
    "json_array(json_extract(raw, '$.url'), json_extract(raw, '$.link'), " +
        "json_extract(raw, '$.jobUrl'), json_extract(raw, '$.job_url'), " +
        "CASE WHEN source = 'AMS' THEN json_extract(raw, '$.externalUrl') END, " +
        "CASE WHEN source = 'AMS' THEN json_extract(raw, '$.portalUrl') END)"

private const val POSTING_ROWS =
    "SELECT id, source, external_id, $POSTING_FIELDS AS posting_urls FROM " +
        "(SELECT id, run_id, source, external_id, CASE WHEN json_valid(raw_result_as_json) " +
        "THEN raw_result_as_json ELSE '{}' END AS raw FROM runner_results)"

/** Reads only saved vacancy URLs when constructing the shared posting index. */
data class VacancyPostingLink(val id: Long, val url: String)

/** Reads small identity projections instead of loading complete dataset descriptions for filtering. */
data class RunnerPostingLink(
    val id: Long,
    val source: String,
    @ColumnInfo(name = "external_id") val externalId: String?,
    @ColumnInfo(name = "posting_urls") val postingUrls: String,
)

/** Shares exact identity resolution between previews, pagination and write transactions. */
interface RunnerVacancyMatchQueries {
    @Query("SELECT id, url FROM vacancies ORDER BY id")
    suspend fun vacancyPostingLinks(): List<VacancyPostingLink>

    @Query("$POSTING_ROWS WHERE run_id = :runId")
    suspend fun runPostingLinks(runId: Long): List<RunnerPostingLink>

    @Query("$POSTING_ROWS WHERE id IN (:ids)")
    suspend fun selectedPostingLinks(ids: List<Long>): List<RunnerPostingLink>

    suspend fun existingVacancies(runId: Long): Map<Long, Long> =
        RunnerPostingIndex(vacancyPostingLinks()).matches(runPostingLinks(runId))

    suspend fun existingVacanciesByIds(ids: List<Long>): Map<Long, Long> = if (ids.isEmpty()) {
        emptyMap()
    } else {
        RunnerPostingIndex(vacancyPostingLinks()).matches(selectedPostingLinks(ids))
    }

    suspend fun existingVacancy(id: Long): Long? = existingVacanciesByIds(listOf(id))[id]
}

/** Builds lookup tables once per read, without persisting inferred import/review history. */
private class RunnerPostingIndex(links: List<VacancyPostingLink>) {
    private val addresses = mutableMapOf<String, Long>()
    private val identities = mutableMapOf<SourcePostingIdentity, Long>()

    init {
        links.forEach { link ->
            postingUrlIdentity(link.url)?.let { identity ->
                addresses.getOrPut(identity.canonicalUrl) { link.id }
                identity.sourceIdentity?.let { identities.getOrPut(it) { link.id } }
            }
        }
    }

    fun matches(rows: List<RunnerPostingLink>): Map<Long, Long> = rows.mapNotNull { row ->
        val urls = (Json.parseToJsonElement(row.postingUrls) as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.takeIf { field -> field.isString }?.content }
            .mapNotNull(::postingUrlIdentity)
        val matched = urls.firstNotNullOfOrNull { url ->
            addresses[url.canonicalUrl] ?: url.sourceIdentity?.let { identities[it] }
        } ?: row.externalId?.let { identities[SourcePostingIdentity(row.source, it)] }
        matched?.let { row.id to it }
    }.toMap()
}
