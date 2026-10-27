package reframecv.providers.apify

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import reframecv.domain.models.providers.AmsJobSearchConfiguration
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerResult
import reframecv.network.createNetworkClient
import reframecv.runners.RunnerExecutionException
import reframecv.runners.VacancySearchResult

class AmsExecutionRepositoryTest {
    private val connection = ProviderConnection(
        name = "Example",
        type = ApiProviderType.Apify,
        configuration = ApifyConfiguration("test-token"),
    )

    @Test
    fun startsAmsWithCompleteDescriptionsExternalSourcesAndExplicitLimits() = runBlocking<Unit> {
        val client = createNetworkClient(
            MockEngine { request ->
                assertEquals(
                    "/v2/actors/blackfalcondata~ams-austria-job-scraper/runs",
                    request.url.encodedPath,
                )
                assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                mapOf(
                    "query" to "Kotlin", "location" to "Wien", "radiusKm" to "20",
                    "maxResults" to "5",
                    "includeExternalJobs" to "true",
                    "includeDetails" to "true",
                    "sortBy" to "date", "mode" to "full", "compact" to "false",
                    "descriptionFormat" to "text", "descriptionMaxLength" to "0",
                    "postedWithin" to "LAST_WEEK",
                ).forEach { (key, value) ->
                    assertEquals(value, body.getValue(key).jsonPrimitive.content)
                }
                assertFalse("actorId" in body)
                respond(
                    """{"data":{"id":"amsRun1","status":"READY",
                    "defaultDatasetId":"data1"}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        try {
            val repository = ApifyExecutionRepository(client)
            assertEquals(
                "amsRun1",
                repository.start(
                    connection,
                    AmsJobSearchConfiguration(
                        query = "Kotlin",
                        location = "Wien",
                        radiusKm = 20,
                        maxResults = 5,
                        postedWithin = "LAST_WEEK",
                    ),
                ).id,
            )
            assertFailsWith<RunnerExecutionException> {
                repository.start(connection, AmsJobSearchConfiguration(query = ""))
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun preservesEveryRawRowAndSeparatesSourcesWithoutMergingSimilarJobs() = runBlocking<Unit> {
        val body = """[
            {"referenceId":"701","title":"Android &#43; Kotlin","employer":"Example &amp; Co",
             "description":"Build&#39;s &#x1F680;\n\nShip &amp; test","externalSource":"devjobs.at",
             "externalUrl":"https://devjobs.at/job/example?utm_source=test",
             "portalUrl":"https://jobs.ams.at/public/emps/jobs/ams1","unknown":{"a":1}},
            {"referenceId":702,"title":"Android &#43; Kotlin","employer":"Example &amp; Co",
             "descriptionHtml":"<p>Work</p><li>Test</li>",
             "externalUrl":"javascript:alert(1)",
             "portalUrl":"https://jobs.ams.at/public/emps/jobs/ams2"},
            {"referenceId":"703","title":{}}, null
        ]"""
        val client = createNetworkClient(
            MockEngine {
                respond(
                    body,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("application/json"),
                        "X-Apify-Pagination-Total" to listOf("9"),
                    ),
                )
            },
        )
        try {
            val page = ApifyExecutionRepository(client).results(
                connection,
                "data1",
                4,
                4,
                ConnectionOperation.AmsJobSearch,
            )
            verifyPage(page)
        } finally {
            client.close()
        }
    }

    @Test
    fun invalidIdentitiesUrlsAndNumericEntitiesDoNotInventData() {
        assertNull(AmsVacancyDto(title = "Job").toDomain())
        assertNull(
            AmsVacancyDto(
                referenceId = kotlinx.serialization.json.JsonPrimitive("1"),
                title = "Job",
                portalUrl = "file:///tmp/example",
            ).toDomain(),
        )
        val dto = AmsVacancyDto(
            referenceId = kotlinx.serialization.json.JsonPrimitive("1"),
            title = "Job",
            portalUrl = "https://jobs.ams.at/job/1",
            publishedDate = "bad-date",
        )
        assertNull(dto.toDomain()!!.postedAt)
        assertEquals(
            "&#0; &#55296; &#99999999; &unknown;",
            cleanText("&#0; &#55296; &#99999999; &unknown;"),
        )
        assertEquals("+ ' \"", cleanText("&#43; &#x27; &#34;"))
    }

    private fun verifyPage(page: reframecv.runners.RunnerResultsPage) {
        assertEquals(8, page.nextOffset)
        assertEquals(9, page.total)
        assertEquals(4, page.rawResults.size)
        assertEquals(listOf(4, 5, 6, 7), page.rawResults.map { it.position })
        assertTrue(page.rawResults.all { it.source == "AMS" })
        assertEquals("701", page.rawResults.first().externalId)
        assertTrue("unknown" in page.rawResults.first().rawResultAsJson)
        assertEquals(2, page.items.size)
        val first = page.items.first() as VacancySearchResult
        assertEquals("Android + Kotlin", first.title)
        assertEquals("Build's 🚀\n\nShip & test", first.description)
        assertEquals("https://devjobs.at/job/example?utm_source=test", first.url)
        val second = page.items.last() as VacancySearchResult
        assertEquals("https://jobs.ams.at/public/emps/jobs/ams2", second.url)
        assertEquals("Work\n- Test", second.description)
        assertNull(second.postedAt)
        val raw = page.rawResults.first()
        val vacancy = ApifyResultConverter().vacancy(
            RunnerResult(
                1,
                2,
                4,
                "AMS",
                raw.externalId,
                raw.rawResultAsJson,
                123,
            ),
            7,
            999,
        )
        assertEquals(7, vacancy.projectId)
        assertEquals("Example & Co", vacancy.company)
        assertEquals(first.description, vacancy.description)
        assertEquals(first.url, vacancy.url)
        assertTrue(vacancy.resumes.isEmpty())
    }
}
