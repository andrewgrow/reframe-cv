package reframecv.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.providers.AmsJobSearchConfiguration
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.LinkedInJobSearchConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.providers.apify.ApifyResultConverter
import reframecv.settings.SettingsBackupJson

class AmsRunnerPersistenceTest : WorkspaceDatabaseTest() {
    @Test
    fun addsAmsWithoutLosingExistingDataAndRestoresItsConfigurationFromBackup() = runBlocking {
        val project = project()
        val resumeId = database.resumeDao().create(resume(project))
        val connections = LocalProviderConnectionsRepository(database.providerConnectionsDao())
        val owner = connections.save(
            ProviderConnection(
                name = "Example",
                type = ApiProviderType.Apify,
                configuration = ApifyConfiguration("test-token"),
            ),
        )
        val runners = LocalConnectionRunnersRepository(database.connectionRunnersDao())
        val linkedIn = runners.save(
            ConnectionRunner(
                connectionId = owner,
                name = "Existing",
                operation = ConnectionOperation.LinkedInJobSearch,
                configuration = RunnerConfigurationJson.encode(
                    LinkedInJobSearchConfiguration(
                        searchUrls = listOf("https://www.linkedin.com/jobs/search"),
                    ),
                ),
            ),
        )
        val config = AmsJobSearchConfiguration(query = "Android", location = "Wien")
        val amsId = runners.save(
            ConnectionRunner(
                connectionId = owner,
                name = "AMS",
                operation = ConnectionOperation.AmsJobSearch,
                configuration = RunnerConfigurationJson.encode(config),
            ),
        )
        val backup = SettingsBackupJson.encode(
            LocalSettingsBackupRepository(database.settingsBackupDao()).snapshot(),
            true,
        )
        val restored = SettingsBackupJson.decode(backup).runners
        assertEquals(2, restored.size)
        assertEquals(
            config,
            RunnerConfigurationJson.decode(
                restored.single {
                    it.operation == ConnectionOperation.AmsJobSearch
                },
            ),
        )
        database.close()
        openDatabase()
        val reopened = LocalConnectionRunnersRepository(database.connectionRunnersDao())
        assertNotNull(reopened.get(linkedIn))
        assertNotNull(database.resumeDao().findById(resumeId))
        val saved = reopened.get(amsId)!!
        assertEquals(config, RunnerConfigurationJson.decode(saved))
        verifyImport(saved, project, reopened)
    }

    private suspend fun verifyImport(
        saved: ConnectionRunner,
        project: Long,
        reopened: LocalConnectionRunnersRepository,
    ) {
        val results = LocalRunnerResultsRepository(
            database.runnerResultsDao(),
            database.runnerResultImportsDao(),
        )
        val runId = reopened.createRun(
            ConnectionRun(
                runnerId = saved.id,
                configurationSnapshot = saved.configuration,
                startedAt = 123,
            ),
        )
        val raw = """{"referenceId":"801","title":"Mobile engineer","employer":"Example Co",
            "description":"Build Kotlin apps","portalUrl":"https://jobs.ams.at/job/801"}"""
        results.savePage(runId, listOf(RunnerRawResult(0, "AMS", "801", raw)), 123)
        val row = results.page(runId, 0, 25).single()
        assertTrue(database.vacancyDao().observeActive(project).first().isEmpty())
        val vacancy = ApifyResultConverter().vacancy(row, project, 456)
        val vacancyId = results.importVacancy(row.id, vacancy)
        assertEquals(
            "Build Kotlin apps",
            database.vacancyDao().findById(vacancyId)!!.entity.description,
        )
        assertEquals(RunnerResultStatus.Imported, results.get(row.id)!!.status)
        assertEquals(raw, results.get(row.id)!!.rawResultAsJson)
    }
}
