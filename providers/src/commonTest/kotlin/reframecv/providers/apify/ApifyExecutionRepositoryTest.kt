package reframecv.providers.apify

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headers
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.LinkedInJobSearchConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.network.HttpTrafficLogger
import reframecv.network.createNetworkClient
import reframecv.runners.RunnerExecutionError
import reframecv.runners.RunnerExecutionException
import reframecv.runners.VacancySearchResult

class ApifyExecutionRepositoryTest {
    private val connection =
        ProviderConnection(
            type = ApiProviderType.Apify,
            name = "Example source",
            configuration = ApifyConfiguration("secret-apify-token"),
        )
    private val configuration =
        LinkedInJobSearchConfiguration(
            searchUrls = listOf("https://www.linkedin.com/jobs/search/?keywords=kotlin"),
            maxResults = 5,
        )
    private fun client(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        total: String? = null,
        request: (HttpRequestData) -> Unit = {},
    ) = createNetworkClient(
        MockEngine { http ->
            request(http)
            respond(
                body,
                status,
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    total?.let { append("X-Apify-Pagination-Total", it) }
                },
            )
        },
    )

    @Test
    fun startUsesBearerAndActorInputNamesAndLogsWithoutCredentials() = runBlocking<Unit> {
        val traffic = mutableListOf<String>()
        val client =
            client(
                """{"data":{"id":"runABC","status":"READY","finishedAt":null,
            "defaultDatasetId":"datasetA","unknown":{"complex":[1,2]}}}""",
            ) { http ->
                assertEquals("POST", http.method.value)
                assertEquals(
                    "/v2/actors/curious_coder~linkedin-jobs-scraper/runs",
                    http.url.encodedPath,
                )
                assertNull(http.url.parameters["token"])
                assertEquals("Bearer secret-apify-token", http.headers[HttpHeaders.Authorization])
                val input = Json.parseToJsonElement((http.body as TextContent).text).jsonObject
                assertEquals(setOf("urls", "limitPerSource"), input.keys)
                assertEquals(5, input.getValue("limitPerSource").jsonPrimitive.int)
            }
        try {
            val result = ApifyExecutionRepository(
                client,
                HttpTrafficLogger {
                    traffic += it
                },
            ).start(connection, configuration)
            assertEquals("runABC", result.id)
            assertEquals(ConnectionRunState.Pending, result.state)
            assertEquals("datasetA", result.datasetId)
            assertNull(result.finishedAt)
            assertEquals(2, traffic.size)
            assertFalse(traffic.joinToString().contains("secret-apify-token"))
            assertTrue(traffic.joinToString().contains("[REDACTED]"))
        } finally {
            client.close()
        }
    }

    @Test
    fun mapsAllRemoteStatesAndOptionalCompletionMetadata() = runBlocking<Unit> {
        val expected = mapOf(
            "READY" to ConnectionRunState.Pending,
            "RUNNING" to ConnectionRunState.Running,
            "ABORTING" to ConnectionRunState.Running,
            "TIMING-OUT" to ConnectionRunState.Running,
            "SUCCEEDED" to ConnectionRunState.Succeeded,
            "FAILED" to ConnectionRunState.Failed,
            "TIMED-OUT" to ConnectionRunState.Failed,
            "ABORTED" to ConnectionRunState.Aborted,
        )
        expected.forEach { (remote, local) ->
            val client =
                client(
                    """{"data":{"id":"runABC","status":"$remote",
                "finishedAt":"2026-10-22T10:00:00Z","usageTotalUsd":0.01005,"statusMessage":"Details"}}""",
                )
            try {
                val result = ApifyExecutionRepository(client).inspect(connection, "runABC")
                assertEquals(local, result.state)
                assertTrue(result.finishedAt!! > 0)
                assertEquals(0.01005, result.costUsd)
                assertEquals("Details", result.details)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun malformedRowsAndDuplicatesDoNotBreakPaginationOrValidNeighbors() = runBlocking<Unit> {
        val client =
            client(
                """[
            {"id":"101","title":"Android &amp; Kotlin","companyName":"ExampleCo",
             "descriptionText":"Intro\n\n\nSkills &amp; tools","location":"Vienna",
             "postedAt":"2026-10-22","salary":"EUR 60000","companyAddress":{"addressCountry":"DE"}},
            {"id":"101","title":"Duplicate"},
            {"id":"102","title":"Mobile engineer","descriptionHtml":"<p>Work</p><ul><li>A &amp; B</li></ul>","salary":[]},
            {"id":"103","title":{}},
            null]""",
                total = "9",
            ) { http ->
                assertEquals("/v2/datasets/datasetA/items", http.url.encodedPath)
                assertEquals("5", http.url.parameters["limit"])
                assertEquals("0", http.url.parameters["offset"])
            }
        try {
            val page = ApifyExecutionRepository(client).results(connection, "datasetA", 0, 5)
            assertEquals(2, page.items.size)
            assertEquals(5, page.nextOffset)
            assertEquals(9, page.total)
            assertEquals(3, page.skipped)
            assertEquals(5, page.rawResults.size)
            val raw = Json.parseToJsonElement(page.rawResults.first().rawResultAsJson).jsonObject
            assertTrue("companyAddress" in raw)
            assertEquals("LinkedIn", page.rawResults.first().source)
            assertEquals("101", page.rawResults.first().externalId)
            assertEquals("null", page.rawResults.last().rawResultAsJson)
            assertEquals(4, page.rawResults.last().position)
            val first = page.items.first() as VacancySearchResult
            assertEquals("Android & Kotlin", first.title)
            assertEquals("Intro\n\nSkills & tools", first.description)
            assertEquals("https://www.linkedin.com/jobs/view/101", first.url)
            assertEquals("Vienna", first.location)
            val vacancy = first.toVacancy(7, 999)
            assertEquals(0L, vacancy.id)
            assertEquals(999L, vacancy.createdAt)
            assertEquals(first.title, vacancy.jobTitle)
            assertTrue(vacancy.requirements.isEmpty())
            assertNull(vacancy.appliedAt)
            assertEquals("Work\n- A & B", (page.items.last() as VacancySearchResult).description)
        } finally {
            client.close()
        }
    }

    @Test
    fun emptyFinalPageAndMissingOptionalFieldsAreSupported() = runBlocking<Unit> {
        val client = client("""[{"id":104,"title":"Minimal","postedAt":"bad-date"}]""")
        try {
            val page = ApifyExecutionRepository(client).results(connection, "datasetA", 50, 50)
            assertNull(page.nextOffset)
            val item = page.items.single() as VacancySearchResult
            assertNull(item.postedAt)
            assertEquals("", item.company)
            assertEquals("", item.description)
        } finally {
            client.close()
        }
        val empty = client("[]", total = "0")
        try {
            assertTrue(
                ApifyExecutionRepository(
                    empty,
                ).results(connection, "datasetA", 0, 50).items.isEmpty(),
            )
        } finally {
            empty.close()
        }
    }

    @Test
    fun errorsPreserveSafeProviderMessagesAndNoPostIsRetried() = runBlocking<Unit> {
        var requests = 0
        val client =
            client(
                """{"error":{"type":"invalid-token","message":"Expired secret-apify-token"}}""",
                HttpStatusCode.Unauthorized,
            ) { requests++ }
        try {
            val error = assertFailsWith<RunnerExecutionException> {
                ApifyExecutionRepository(client).start(connection, configuration)
            }
            assertEquals(RunnerExecutionError.Unauthorized, error.reason)
            assertEquals("Expired [REDACTED]", error.details)
            assertNull(error.cause)
            assertEquals(1, requests)
        } finally {
            client.close()
        }
        listOf(
            "not JSON",
            "{}",
            """{"data":{"id":"runABC","status":"FUTURE"}}""",
        ).forEach { response ->
            val invalid = client(response)
            try {
                assertEquals(
                    RunnerExecutionError.InvalidResponse,
                    assertFailsWith<RunnerExecutionException> {
                        ApifyExecutionRepository(invalid).inspect(connection, "runABC")
                    }.reason,
                )
            } finally {
                invalid.close()
            }
        }
    }

    @Test
    fun disabledWrongProviderAndUnsafeIdentifiersNeverReachHttpAndCancellationPropagates() =
        runBlocking<Unit> {
            var calls = 0
            val client = client("{}") { calls++ }
            try {
                listOf(
                    connection.copy(active = false),
                    connection.copy(
                        type = ApiProviderType.OpenRouter,
                        configuration = OpenRouterConfiguration("key"),
                    ),
                ).forEach {
                    assertFailsWith<RunnerExecutionException> {
                        ApifyExecutionRepository(client).start(it, configuration)
                    }
                }
                assertFailsWith<RunnerExecutionException> {
                    ApifyExecutionRepository(client).inspect(connection, "../other")
                }
                assertEquals(0, calls)
            } finally {
                client.close()
            }
            val cancelled =
                createNetworkClient(MockEngine { throw CancellationException("cancelled") })
            try {
                assertFailsWith<CancellationException> {
                    ApifyExecutionRepository(cancelled).start(connection, configuration)
                }
            } finally {
                cancelled.close()
            }
        }
}
