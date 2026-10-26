package reframecv.database.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

class RunnerResultsTest : WorkspaceDatabaseTest() {
    private val payload = """{"id":"101","title":"Example role","unknown":{"values":[1,null]}}"""
    private fun raw(position: Int = 0, source: String = "LinkedIn", id: String? = "101") =
        RunnerRawResult(position, source, id, payload)
    private fun results() =
        LocalRunnerResultsRepository(database.runnerResultsDao(), database.runnerResultImportsDao())
    private suspend fun run(): ConnectionRun {
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
                name = "Search",
                operation = ConnectionOperation.LinkedInJobSearch,
                configuration = "{}",
            ),
        )
        val run = ConnectionRun(runnerId = runner, configurationSnapshot = "{}", startedAt = 100)
        return run.copy(id = runners.createRun(run))
    }

    @Test
    fun pageReadsAreIdempotentAndNeverOverwriteOriginalSnapshotsOrReview() = runBlocking {
        val run = run()
        val repository = results()
        repository.savePage(run.id, listOf(raw(), raw(1), raw(2, id = null)), 200)
        val saved = repository.page(run.id, 0, 50)
        assertEquals(2, saved.size)
        assertEquals(payload, saved.first().rawResultAsJson)
        assertNull(saved.last().externalId)
        assertNull(saved.first().relevanceScore)
        repository.setRelevance(saved.first().id, run.id, 0, "Android roles")
        assertFailsWith<IllegalStateException> {
            repository.setRelevance(saved.first().id, run.id + 1, 90, "Wrong run")
        }
        assertFailsWith<IllegalArgumentException> {
            repository.setRelevance(saved.first().id, run.id, 101, "Invalid score")
        }
        repository.setStatus(saved.first().id, RunnerResultStatus.Dismissed)
        repository.savePage(
            run.id,
            listOf(raw().copy(rawResultAsJson = "{}"), raw(2, id = null)),
            999,
        )
        val reread = repository.page(run.id, 0, 50)
        assertEquals(saved.map { it.id }, reread.map { it.id })
        assertEquals(200, reread.first().receivedAt)
        assertEquals(payload, reread.first().rawResultAsJson)
        assertEquals(0, reread.first().relevanceScore)
        assertEquals("Android roles", reread.first().relevancePrompt)
        assertNull(reread.last().relevanceScore)
        repository.setRelevance(saved.first().id, run.id, 90, "Revised criteria")
        assertEquals(90, repository.get(saved.first().id)?.relevanceScore)
        assertEquals("Revised criteria", repository.get(saved.first().id)?.relevancePrompt)
        assertEquals(RunnerResultStatus.Dismissed, reread.first().status)
        repository.setStatus(saved.first().id, RunnerResultStatus.New)
        assertEquals(RunnerResultStatus.New, repository.page(run.id, 0, 1).single().status)
    }

    @Test
    fun identifiesRepeatsAcrossConnectionsButSeparatesSourcesAndUnknownIdentities() =
        runBlocking<Unit> {
            val first = run()
            val second = run()
            val repository = results()
            repository.savePage(first.id, listOf(raw(), raw(1, id = null)), 200)
            repository.savePage(second.id, listOf(raw(), raw(1, "Other"), raw(2, id = null)), 300)
            val previous = repository.page(first.id, 0, 50).first()
            val current = repository.page(second.id, 0, 50)
            assertEquals(previous.id, current.first().previousResultId)
            assertEquals(RunnerResultStatus.New, current.first().previousStatus)
            assertNull(current[1].previousResultId)
            assertNull(current[2].previousResultId)
            assertNull(previous.previousResultId)
            assertTrue(repository.page(second.id, 50, 50).isEmpty())
        }

    @Test
    fun importedIdentitySurvivesVacancyDeletionAndNewerUnreviewedOccurrences() = runBlocking<Unit> {
        val first = run()
        val repository = results()
        repository.savePage(first.id, listOf(raw()), 100)
        val original = repository.page(first.id, 0, 1).single()
        val project = project()
        val vacancy = database.vacancyDao().create(vacancy(project))
        repository.setStatus(original.id, RunnerResultStatus.Imported, vacancy)
        val second = run()
        repository.savePage(second.id, listOf(raw()), 200)
        val third = run()
        repository.savePage(third.id, listOf(raw()), 300)
        assertEquals(vacancy, repository.page(third.id, 0, 1).single().previousVacancyId)
        database.vacancyDao().delete(vacancy, 400)
        val historical = repository.page(first.id, 0, 1).single()
        assertNull(historical.vacancyId)
        assertEquals(RunnerResultStatus.Imported, historical.status)
        val repeated = repository.page(third.id, 0, 1).single()
        assertEquals(original.id, repeated.previousResultId)
        assertEquals(RunnerResultStatus.Imported, repeated.previousStatus)
        assertNull(repeated.previousVacancyId)
        assertFailsWith<IllegalArgumentException> {
            repository.setStatus(original.id, RunnerResultStatus.New)
        }
    }

    @Test
    fun historyDependsOnRunNotProjectAndRunnerDeletionCascades() = runBlocking {
        val run = run()
        val repository = results()
        repository.savePage(run.id, listOf(raw()), 100)
        val record = repository.page(run.id, 0, 1).single()
        val project = project()
        val vacancy = database.vacancyDao().create(vacancy(project))
        repository.setStatus(record.id, RunnerResultStatus.Imported, vacancy)
        database.projectDao().deleteSubtree(project)
        assertNull(repository.page(run.id, 0, 1).single().vacancyId)
        LocalConnectionRunnersRepository(database.connectionRunnersDao()).delete(run.runnerId)
        assertTrue(repository.page(run.id, 0, 50).isEmpty())
    }

    @Test
    fun paginatesRawPositionsAndRejectsInvalidImportLinks() = runBlocking<Unit> {
        val run = run()
        val repository = results()
        repository.savePage(run.id, listOf(raw(0), raw(50, id = "102")), 100)
        assertEquals(listOf(0), repository.page(run.id, 0, 50).map { it.position })
        val second = repository.page(run.id, 50, 50).single()
        assertEquals("102", second.externalId)
        assertFailsWith<IllegalArgumentException> {
            repository.setStatus(second.id, RunnerResultStatus.Imported)
        }
        assertFailsWith<IllegalArgumentException> {
            repository.setStatus(second.id, RunnerResultStatus.Dismissed, 99)
        }
        assertFailsWith<IllegalStateException> {
            repository.setStatus(999, RunnerResultStatus.Dismissed)
        }
        assertNotNull(database.runnerResultsDao().get(second.id))
    }

    @Test
    fun rawSnapshotsAndReviewSurviveReopeningTheDatabase() = runBlocking {
        val run = run()
        results().savePage(run.id, listOf(raw()), 100)
        val original = results().page(run.id, 0, 1).single()
        results().setStatus(original.id, RunnerResultStatus.Dismissed)
        results().setRelevance(original.id, run.id, 83, "Saved relevance criteria")
        database.close()
        database = buildDatabase(
            createDatabaseBuilder(
                temporaryFolder.root.toPath().resolve("workspace.db"),
            ),
        )
        val restored = results().page(run.id, 0, 1).single()
        assertEquals(original.id, restored.id)
        assertEquals(83, restored.relevanceScore)
        assertEquals("Saved relevance criteria", restored.relevancePrompt)
        assertEquals(payload, restored.rawResultAsJson)
        assertEquals(RunnerResultStatus.Dismissed, restored.status)
    }

    @Test
    fun filtersBeforePagingAndRestoresRowsWithoutChangingSourcePositions() = runBlocking {
        val run = run()
        val repository = results()
        repository.savePage(run.id, (0..59).map { raw(it, id = "$it") }, 200)
        val rows = repository.reviewPage(run.id, 0, 100)
        val vacancy = database.vacancyDao().create(vacancy(project()))
        rows.take(30).forEach { repository.setStatus(it.id, RunnerResultStatus.Dismissed) }
        rows.drop(30).take(15).forEach {
            repository.setStatus(it.id, RunnerResultStatus.Imported, vacancy)
        }
        val newOnly = RunnerResultFilter(showDismissed = false, showImported = false)
        assertEquals(15, repository.count(run.id, newOnly))
        assertEquals(
            (45..59).toList(),
            repository.reviewPage(run.id, 0, 25, newOnly).map {
                it.position
            },
        )
        val noImports = RunnerResultFilter(showImported = false)
        assertEquals(45, repository.count(run.id, noImports))
        assertEquals(
            (0..24).toList(),
            repository.reviewPage(run.id, 0, 25, noImports).map {
                it.position
            },
        )
        assertEquals(
            (25..29).toList() + (45..59).toList(),
            repository.reviewPage(run.id, 25, 25, noImports).map { it.position },
        )
        val noDismissals = RunnerResultFilter(showDismissed = false)
        assertEquals(30, repository.count(run.id, noDismissals))
        assertEquals(60, repository.count(run.id))
        repository.setStatus(rows.first().id, RunnerResultStatus.New)
        assertEquals(16, repository.count(run.id, newOnly))
        assertEquals(0, repository.reviewPage(run.id, 0, 25, newOnly).first().position)
    }

    @Test
    fun filtersHistoricalDecisionsUsingTheSamePrecedenceAsDuplicateIndicators() = runBlocking {
        val first = run()
        val repository = results()
        repository.savePage(first.id, listOf(raw(), raw(1, id = "102")), 100)
        val previous = repository.reviewPage(first.id, 0, 25)
        val vacancy = database.vacancyDao().create(vacancy(project()))
        repository.setStatus(previous[0].id, RunnerResultStatus.Imported, vacancy)
        repository.setStatus(previous[1].id, RunnerResultStatus.Dismissed)
        val second = run()
        repository.savePage(
            second.id,
            listOf(raw(), raw(1, id = "102"), raw(2, id = null)),
            200,
        )
        val filter = RunnerResultFilter(showDismissed = false, showImported = false)
        assertEquals(1, repository.count(second.id, filter))
        assertEquals(2, repository.reviewPage(second.id, 0, 25, filter).single().position)
        val current = repository.reviewPage(second.id, 0, 25)
        assertEquals(RunnerResultStatus.Imported, current[0].previousStatus)
        assertEquals(RunnerResultStatus.Dismissed, current[1].previousStatus)
        assertFailsWith<IllegalArgumentException> {
            repository.setStatus(current[0].id, RunnerResultStatus.Dismissed)
        }
        assertEquals(2, repository.count(second.id, RunnerResultFilter(showImported = false)))
        assertEquals(1, repository.count(second.id, filter))
        assertEquals(3, repository.count(second.id))
    }
}
