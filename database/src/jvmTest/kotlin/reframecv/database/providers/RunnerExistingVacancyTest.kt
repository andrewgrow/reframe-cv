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

class RunnerExistingVacancyTest : WorkspaceDatabaseTest() {
    private fun results() = LocalRunnerResultsRepository(
        database.runnerResultsDao(),
        database.runnerResultImportsDao(),
    )

    private suspend fun run(): Long {
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
                operation = ConnectionOperation.LinkedInJobSearch,
                configuration = "{}",
            ),
        )
        val run = runners.createRun(
            ConnectionRun(
                runnerId = runner,
                configurationSnapshot = "{}",
                startedAt = 100,
            ),
        )
        results().savePage(
            run,
            listOf("101", "102", "103").mapIndexed { position, id ->
                RunnerRawResult(position, "LinkedIn", id, """{"id":"$id","title":"Example"}""")
            },
            200,
        )
        return run
    }

    @Test
    fun existingVacancyOverridesSeenAndDismissedHistoryAndFiltersBeforePagination() = runBlocking {
        val older = run()
        val oldRow = results().page(older, 0, 10).first()
        results().setStatus(oldRow.id, RunnerResultStatus.Dismissed)
        val current = run()
        val row = results().page(current, 0, 10).first()
        val project = project()
        val saved = database.vacancyDao().create(
            vacancy(project).copy(
                url = "https://www.linkedin.com/jobs/view/101",
            ),
        )
        val matched = results().get(row.id)!!
        assertEquals(RunnerResultStatus.Imported, matched.reviewStatus)
        assertEquals(saved, matched.existingVacancyId)
        assertEquals(saved, matched.reviewVacancyId)
        assertEquals(RunnerResultStatus.New, matched.status)
        assertEquals(row.rawResultAsJson, matched.rawResultAsJson)
        assertEquals(saved, results().get(oldRow.id)?.existingVacancyId)
        val visible = RunnerResultFilter(showDismissed = false, showImported = false)
        assertEquals(2, results().count(current, visible))
        assertEquals("102", results().reviewPage(current, 0, 1, visible).single().externalId)
        assertEquals("103", results().reviewPage(current, 1, 1, visible).single().externalId)
        assertEquals(3, results().count(current))
        assertFailsWith<IllegalArgumentException> {
            results().importVacancy(row.id, vacancy(project))
        }
        assertFailsWith<IllegalArgumentException> {
            results().setStatus(row.id, RunnerResultStatus.Dismissed)
        }
        assertFailsWith<IllegalArgumentException> {
            results().setStatus(row.id, RunnerResultStatus.Restored)
        }
        assertEquals(RunnerResultStatus.New.name, database.runnerResultsDao().get(row.id)?.status)
        database.close()
        openDatabase()
        assertEquals(saved, results().get(row.id)?.existingVacancyId)
    }

    @Test
    fun trackingSlugFragmentsRegionalHostsAndCaseKeepTheSameExactIdentity() = runBlocking {
        val row = results().page(run(), 0, 10).first()
        val project = project()
        val links = listOf(
            "https://linkedin.com/jobs/view/101",
            "http://www.linkedin.com/jobs/view/101/?tracking=1#details",
            " https://AT.LINKEDIN.COM/jobs/view/example-role-101?position=3 ",
            "https://www.linkedin.com/jobs/view/example-role-101/#details?tracking=1",
        )
        for (link in links) {
            val saved = database.vacancyDao().create(vacancy(project).copy(url = link))
            assertEquals(saved, results().get(row.id)?.existingVacancyId, link)
            database.vacancyDao().delete(saved, 400)
            assertNull(results().get(row.id)?.existingVacancyId)
            assertEquals(RunnerResultStatus.New, results().get(row.id)?.reviewStatus)
        }
    }

    @Test
    fun similarTitlesPartialIdsForeignHostsAndWrongSourcesAreNotMatches() = runBlocking {
        val run = run()
        val row = results().page(run, 0, 10).first()
        val project = project()
        val links = listOf(
            "https://www.linkedin.com/jobs/view/1010",
            "https://www.linkedin.com/jobs/view/example-1101",
            "https://www.linkedin.com/jobs/view/101/another-path",
            "https://www.linkedin.com.evil.example/jobs/view/101",
            "https://example.com/.linkedin.com/jobs/view/101",
            "https://example.com/jobs/view/101?url=https://www.linkedin.com/jobs/view/101",
            "https://www.linkedin.com/jobs/search?currentJobId=101",
        )
        for (link in links) {
            database.vacancyDao().create(vacancy(project).copy(name = "Example", url = link))
        }
        assertNull(results().get(row.id)?.existingVacancyId)
        assertEquals(3, results().count(run, RunnerResultFilter(showImported = false)))
        database.vacancyDao().create(
            vacancy(project).copy(
                url = "https://www.linkedin.com/jobs/view/101",
            ),
        )
        results().savePage(
            run,
            listOf(
                RunnerRawResult(3, "Other", "101", "{}"),
                RunnerRawResult(4, "LinkedIn", null, "{}"),
                RunnerRawResult(5, "LinkedIn", "%", "{}"),
            ),
            200,
        )
        results().page(run, 3, 10).forEach { assertNull(it.existingVacancyId) }
    }
}
