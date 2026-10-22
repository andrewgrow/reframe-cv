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
    fun pageReadsAreIdempotentAndNeverOverwriteOriginalSnapshotsOrReview() = runBlocking<Unit> {
        val run = run()
        val repository = results()
        repository.savePage(run.id, listOf(raw(), raw(1), raw(2, id = null)), 200)
        val saved = repository.page(run.id, 0, 50)
        assertEquals(2, saved.size)
        assertEquals(payload, saved.first().rawResultAsJson)
        assertNull(saved.last().externalId)
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
    fun historyDependsOnRunNotProjectAndRunnerDeletionCascades() = runBlocking<Unit> {
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
    fun rawSnapshotsAndReviewSurviveReopeningTheDatabase() = runBlocking<Unit> {
        val run = run()
        results().savePage(run.id, listOf(raw()), 100)
        val original = results().page(run.id, 0, 1).single()
        results().setStatus(original.id, RunnerResultStatus.Dismissed)
        database.close()
        database = buildDatabase(
            createDatabaseBuilder(
                temporaryFolder.root.toPath().resolve("workspace.db"),
            ),
        )
        val restored = results().page(run.id, 0, 1).single()
        assertEquals(original.id, restored.id)
        assertEquals(payload, restored.rawResultAsJson)
        assertEquals(RunnerResultStatus.Dismissed, restored.status)
    }
}
