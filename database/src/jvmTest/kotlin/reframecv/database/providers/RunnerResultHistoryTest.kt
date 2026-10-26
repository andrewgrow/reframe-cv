package reframecv.database.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
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

class RunnerResultHistoryTest : WorkspaceDatabaseTest() {
    private fun results() = LocalRunnerResultsRepository(
        database.runnerResultsDao(),
        database.runnerResultImportsDao(),
    )
    private suspend fun occurrence(externalId: String? = "101", source: String = "LinkedIn") = run {
        val owner = LocalProviderConnectionsRepository(database.providerConnectionsDao()).save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example source",
                configuration = ApifyConfiguration("fictional-token"),
            ),
        )
        val runners = LocalConnectionRunnersRepository(database.connectionRunnersDao())
        val runner = runners.save(
            ConnectionRunner(
                connectionId = owner,
                name = "Example search",
                operation = ConnectionOperation.LinkedInJobSearch,
                configuration = "{}",
            ),
        )
        val run = runners.createRun(
            ConnectionRun(runnerId = runner, configurationSnapshot = "{}", startedAt = 100),
        )
        results().savePage(
            run,
            listOf(RunnerRawResult(0, source, externalId, """{"title":"Example role"}""")),
            200,
        )
        results().page(run, 0, 1).single()
    }

    @Test
    fun explicitRestoreSurvivesUnreviewedRunsReopeningAndAllowsImport() = runBlocking<Unit> {
        val first = occurrence()
        results().setStatus(first.id, RunnerResultStatus.Dismissed)
        occurrence()
        val third = occurrence()
        assertEquals(RunnerResultStatus.Dismissed, third.reviewStatus)
        assertEquals(0, results().count(third.runId, RunnerResultFilter(showDismissed = false)))
        results().setStatus(third.id, RunnerResultStatus.Restored)
        assertEquals(RunnerResultStatus.New, results().get(third.id)?.reviewStatus)
        assertEquals(1, results().count(third.runId, RunnerResultFilter(showDismissed = false)))
        val fourth = occurrence()
        assertEquals(RunnerResultStatus.New, fourth.reviewStatus)
        database.close()
        database =
            buildDatabase(
                createDatabaseBuilder(temporaryFolder.root.toPath().resolve("workspace.db")),
            )
        assertEquals(RunnerResultStatus.New, results().get(fourth.id)?.reviewStatus)
        val vacancy = results().importVacancy(fourth.id, vacancy(project()))
        assertEquals(vacancy, results().get(fourth.id)?.reviewVacancyId)
        assertEquals(RunnerResultStatus.Imported, results().get(fourth.id)?.reviewStatus)
    }

    @Test
    fun importedHistoryCannotBeDismissedRestoredOrImportedAgainEvenAfterVacancyDeletion() =
        runBlocking<Unit> {
            val older = occurrence()
            val first = occurrence()
            val target = project()
            val vacancy = results().importVacancy(first.id, vacancy(target))
            assertEquals(RunnerResultStatus.Imported, results().get(older.id)?.reviewStatus)
            assertEquals(0, results().count(older.runId, RunnerResultFilter(showImported = false)))
            assertFailsWith<IllegalArgumentException> {
                results().setStatus(older.id, RunnerResultStatus.Dismissed)
            }
            assertFailsWith<IllegalArgumentException> {
                results().importVacancy(older.id, vacancy(target))
            }
            occurrence()
            val repeated = occurrence()
            assertEquals(RunnerResultStatus.Imported, repeated.reviewStatus)
            assertEquals(vacancy, repeated.reviewVacancyId)
            assertEquals(
                0,
                results().count(repeated.runId, RunnerResultFilter(showImported = false)),
            )
            for (status in listOf(RunnerResultStatus.Dismissed, RunnerResultStatus.Restored)) {
                assertFailsWith<IllegalArgumentException> {
                    results().setStatus(repeated.id, status)
                }
            }
            assertFailsWith<IllegalArgumentException> {
                results().importVacancy(repeated.id, vacancy(target))
            }
            assertEquals(1, database.vacancyDao().observeActive(target).first().size)
            database.vacancyDao().delete(vacancy, 400)
            assertEquals(RunnerResultStatus.Imported, results().get(repeated.id)?.reviewStatus)
            assertNull(results().get(repeated.id)?.reviewVacancyId)
            assertFailsWith<IllegalArgumentException> {
                results().importVacancy(repeated.id, vacancy(target))
            }
        }

    @Test
    fun latestEarlierAssessmentIsIndependentOfReviewAndIdentitySafe() = runBlocking<Unit> {
        val first = occurrence()
        results().setStatus(first.id, RunnerResultStatus.Dismissed)
        results().setRelevance(first.id, first.runId, 91, "First criteria")
        val second = occurrence()
        results().setRelevance(second.id, second.runId, 0, "Latest criteria")
        val third = occurrence()
        assertEquals(RunnerResultStatus.Dismissed, third.reviewStatus)
        assertEquals(0, third.relevanceScore)
        assertEquals("Latest criteria", third.relevancePrompt)
        assertNull(database.runnerResultsDao().get(third.id)?.relevanceScore)
        assertEquals(0, results().reviewPage(third.runId, 0, 25).single().relevanceScore)
        results().setRelevance(third.id, third.runId, 88, "Own criteria")
        results().setRelevance(first.id, first.runId, 60, "Changed older criteria")
        assertEquals(88, results().get(third.id)?.relevanceScore)
        occurrence()
        val fifth = occurrence()
        assertEquals(88, fifth.relevanceScore)
        assertEquals("Own criteria", fifth.relevancePrompt)
        assertNull(occurrence(externalId = "other").relevanceScore)
        assertNull(occurrence(source = "Other source").relevanceScore)
        assertNull(occurrence(externalId = null).relevanceScore)
    }
}
