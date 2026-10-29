package reframecv.database.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.repository.LocalConnectionRunnersRepository
import reframecv.repository.LocalProviderConnectionsRepository
import reframecv.repository.LocalRunnerResultsRepository

class RunnerGenericVacancyMatchTest : WorkspaceDatabaseTest() {
    private fun results() = LocalRunnerResultsRepository(
        database.runnerResultsDao(),
        database.runnerResultImportsDao(),
    )
    private suspend fun run(rows: List<RunnerRawResult>): Long {
        val connection = LocalProviderConnectionsRepository(database.providerConnectionsDao()).save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example",
                configuration = ApifyConfiguration("fictional-token"),
            ),
        )
        val runners = LocalConnectionRunnersRepository(database.connectionRunnersDao())
        val runner = runners.save(
            ConnectionRunner(
                connectionId = connection,
                name = "Example",
                operation = ConnectionOperation.AmsJobSearch,
                configuration = "{}",
            ),
        )
        val id = runners.createRun(
            ConnectionRun(
                runnerId = runner,
                configurationSnapshot = "{}",
                startedAt = 100,
            ),
        )
        results().savePage(id, rows, 200)
        return id
    }

    @Test
    fun bothSourceAliasesResolveAndUnknownScrapersCanUseCanonicalPostingUrls() = runBlocking {
        val target = project()
        val saved = database.vacancyDao().create(
            vacancy(target).copy(
                url = "https://careers.example.test/Jobs?jobId=101&lang=en",
            ),
        )
        val run = run(
            listOf(
                RunnerRawResult(
                    0,
                    "AMS",
                    "801",
                    """{"externalUrl":
                "https://CAREERS.example.test/Jobs/?lang=en&jobId=101&utm_source=board",
                "portalUrl":"https://jobs.ams.at/public/emps/jobs/801"}""",
                ),
                RunnerRawResult(
                    1,
                    "Future source",
                    "unrelated-id",
                    """{"url":
                "https://careers.example.test/Jobs?jobId=101&lang=en#details"}""",
                ),
                RunnerRawResult(
                    2,
                    "Other source",
                    "101",
                    """{"jobUrl":
                "https://careers.example.test/Jobs?jobId=102&lang=en"}""",
                ),
            ),
        )
        val rows = results().page(run, 0, 10)
        assertEquals(listOf(saved, saved, null), rows.map { it.existingVacancyId })
        val filter = RunnerResultFilter(showImported = false)
        assertEquals(1, results().count(run, filter))
        assertEquals(rows.last().id, results().reviewPage(run, 0, 1, filter).single().id)
        assertFailsWith<IllegalArgumentException> {
            results().importVacancy(rows.first().id, vacancy(target))
        }
        assertFailsWith<IllegalArgumentException> {
            results().setStatus(rows[1].id, RunnerResultStatus.Dismissed)
        }
        database.vacancyDao().delete(saved, 400)
        val portal = database.vacancyDao().create(
            vacancy(target).copy(
                url = "https://jobs.ams.at/public/emps/jobs/801",
            ),
        )
        assertEquals(portal, results().get(rows.first().id)?.existingVacancyId)
        assertNull(results().get(rows[1].id)?.existingVacancyId)
    }

    @Test
    fun bareIdsAreSourceScopedAndCompanyApplyAndSearchUrlsNeverBecomePostingMatches() =
        runBlocking {
            val target = project()
            val saved = database.vacancyDao().create(
                vacancy(target).copy(
                    url = "https://jobs.ams.at/public/emps/jobs/101?tracking=abc",
                ),
            )
            val run = run(
                listOf(
                    RunnerRawResult(0, "AMS", "101", "{}"),
                    RunnerRawResult(1, "LinkedIn", "101", "{}"),
                    RunnerRawResult(
                        2,
                        "Unknown",
                        "101",
                        """{"companyWebsite":
                "https://jobs.ams.at/public/emps/jobs/101?tracking=abc",
                "applyUrl":"https://jobs.ams.at/public/emps/jobs/101?tracking=abc",
                "inputUrl":"https://jobs.ams.at/public/emps/jobs/101?tracking=abc"}""",
                    ),
                    RunnerRawResult(
                        3,
                        "Unknown",
                        "102",
                        """{"url":{},"link":false,"jobUrl":[]} """,
                    ),
                    RunnerRawResult(4, "Unknown", "103", "not JSON"),
                ),
            )
            assertEquals(
                listOf(saved, null, null, null, null),
                results().page(run, 0, 10).map { it.existingVacancyId },
            )
            assertEquals(4, results().count(run, RunnerResultFilter(showImported = false)))
        }
}
