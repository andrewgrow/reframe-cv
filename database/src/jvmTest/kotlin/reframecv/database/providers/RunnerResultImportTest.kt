package reframecv.database.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.repository.LocalConnectionRunnersRepository
import reframecv.repository.LocalProviderConnectionsRepository
import reframecv.repository.LocalRunnerResultsRepository

class RunnerResultImportTest : WorkspaceDatabaseTest() {
    private fun repository() =
        LocalRunnerResultsRepository(database.runnerResultsDao(), database.runnerResultImportsDao())
    private suspend fun source(): RunnerResult {
        val owner = LocalProviderConnectionsRepository(
            database.providerConnectionsDao(),
        ).save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example",
                configuration = ApifyConfiguration("token"),
            ),
        )
        val runners = LocalConnectionRunnersRepository(database.connectionRunnersDao())
        val runner = runners.save(
            ConnectionRunner(
                connectionId = owner,
                name = "Example",
                operation = ConnectionOperation.LinkedInJobSearch,
                configuration = "{}",
            ),
        )
        val run = runners.createRun(
            ConnectionRun(runnerId = runner, configurationSnapshot = "{}", startedAt = 100),
        )
        repository().savePage(
            run,
            listOf(RunnerRawResult(0, "LinkedIn", "101", """{"id":"101","title":"Example"}""")),
            200,
        )
        return repository().page(run, 0, 50).single()
    }

    @Test
    fun successfulImportLinksOneEditableVacancyAndKeepsTheOriginalPayload() = runBlocking<Unit> {
        val source = source()
        val target = project()
        val id = repository().importVacancy(source.id, vacancy(target))
        assertEquals(id, repository().get(source.id)?.vacancyId)
        assertEquals(RunnerResultStatus.Imported, repository().get(source.id)?.status)
        assertEquals(source.rawResultAsJson, repository().get(source.id)?.rawResultAsJson)
        assertNotNull(database.vacancyDao().findById(id))
        assertFailsWith<IllegalArgumentException> {
            repository().importVacancy(source.id, vacancy(target))
        }
        assertEquals(1, database.vacancyDao().observeActive(target).first().size)
    }

    @Test
    fun rejectedAndInvalidProjectImportsRollBackWithoutCreatingRecords() = runBlocking<Unit> {
        val source = source()
        val target = project()
        repository().setStatus(source.id, RunnerResultStatus.Dismissed)
        assertFailsWith<IllegalArgumentException> {
            repository().importVacancy(source.id, vacancy(target))
        }
        repository().setStatus(source.id, RunnerResultStatus.New)
        assertFailsWith<IllegalStateException> {
            repository().importVacancy(source.id, vacancy(999))
        }
        assertEquals(RunnerResultStatus.New, repository().get(source.id)?.status)
        assertEquals(0, database.vacancyDao().observeActive(target).first().size)
    }

    @Test
    fun cursorAndRowsAreCommittedTogetherAndStaleCursorsCannotOverwriteProgress() =
        runBlocking<Unit> {
            val source = source()
            val runners = LocalConnectionRunnersRepository(database.connectionRunnersDao())
            val run = runners.getRun(source.runId)!!
            repository().commitPage(
                run.copy(
                    nextResultOffset = 50,
                    resultTotal = 75,
                    resultColumns = listOf("id", "title"),
                ),
                emptyList(),
                300,
            )
            assertEquals(50, runners.getRun(run.id)?.nextResultOffset)
            assertFailsWith<IllegalStateException> {
                repository().commitPage(
                    run.copy(nextResultOffset = 25),
                    listOf(RunnerRawResult(1, "LinkedIn", "102", "{}")),
                    400,
                )
            }
            assertEquals(1, repository().count(run.id))
            runners.updateRun(run.copy(error = "Example status"))
            assertEquals(50, runners.getRun(run.id)?.nextResultOffset)
            assertEquals(listOf("id", "title"), runners.getRun(run.id)?.resultColumns)
        }
}
