package reframecv.database.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.InvalidProviderConfiguration
import reframecv.domain.models.providers.LinkedInJobSearchConfiguration
import reframecv.domain.models.providers.ProviderConfigurationJson
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.repository.LocalConnectionRunnersRepository
import reframecv.repository.LocalProviderConnectionsRepository
import reframecv.repository.LocalSettingsBackupRepository
import reframecv.settings.SettingsBackupJson
import reframecv.usecase.ConnectionRunnersUseCases

class ConnectionRunnersTest : WorkspaceDatabaseTest() {
    private val configuration = RunnerConfigurationJson.encode(
        LinkedInJobSearchConfiguration(
            searchUrls = listOf("https://www.linkedin.com/jobs/search/"),
        ),
    )
    private suspend fun connection() =
        LocalProviderConnectionsRepository(database.providerConnectionsDao())
            .save(
                ProviderConnection(
                    type = ApiProviderType.Apify,
                    name = "Apify",
                    configuration = ApifyConfiguration("SECRET_TOKEN"),
                ),
            )

    @Test
    fun runnerChangesDoNotRewriteRunInputsAndDisabledConnectionsCannotStartRuns() =
        runBlocking<Unit> {
            val connections = LocalProviderConnectionsRepository(database.providerConnectionsDao())
            val repository = LocalConnectionRunnersRepository(database.connectionRunnersDao())
            val useCases = ConnectionRunnersUseCases(repository, connections)
            val owner = connection()
            val id = useCases.save(
                ConnectionRunner(
                    connectionId = owner,
                    name = "Search",
                    operation = ConnectionOperation.LinkedInJobSearch,
                    configuration = configuration,
                ),
            )
            val runId = useCases.prepareRun(id, 123)
            val original = repository.get(id)!!
            useCases.save(
                original.copy(
                    configuration = RunnerConfigurationJson.encode(
                        LinkedInJobSearchConfiguration(searchUrls = listOf("another search")),
                    ),
                ),
            )
            val run = repository.runs(id).first().single()
            assertEquals(runId, run.id)
            assertEquals(configuration, run.configurationSnapshot)
            assertFalse("SECRET_TOKEN" in run.configurationSnapshot)
            repository.updateRun(
                run.copy(
                    state = ConnectionRunState.Succeeded,
                    externalId = "remote-id",
                    finishedAt = 456,
                ),
            )
            assertEquals("remote-id", repository.runs(id).first().single().externalId)
            assertEquals("remote-id", repository.getRun(runId)?.externalId)
            assertFailsWith<IllegalArgumentException> {
                repository.updateRun(run.copy(configurationSnapshot = "{}"))
            }
            connections.setActive(owner, false)
            assertFailsWith<IllegalArgumentException> { useCases.prepareRun(id, 789) }
            useCases.delete(id)
            assertTrue(repository.runs(id).first().isEmpty())
        }

    @Test
    fun backupsRemapRunnerOwnershipExcludeHistoryAndRedactApifyTokens() = runBlocking<Unit> {
        val owner = connection()
        val repository = LocalConnectionRunnersRepository(database.connectionRunnersDao())
        repository.save(
            ConnectionRunner(
                connectionId = owner,
                name = "Search",
                operation = ConnectionOperation.LinkedInJobSearch,
                configuration = configuration,
            ),
        )
        val backups = LocalSettingsBackupRepository(database.settingsBackupDao())
        val portable = SettingsBackupJson.encode(backups.snapshot(), false)
        assertFalse("SECRET_TOKEN" in portable)
        assertFalse("apiToken" in portable)
        assertFalse("configurationSnapshot" in portable)
        val decoded = SettingsBackupJson.decode(portable)
        val report = backups.restore(decoded)
        assertEquals(0, report.connections)
        assertEquals(1, repository.observe(owner).first().size)
        val json = SettingsBackupJson.encode(backups.snapshot(), true)
        assertTrue("SECRET_TOKEN" in json)
        assertEquals(configuration, SettingsBackupJson.decode(json).runners.single().configuration)
    }

    @Test
    fun brokenAndFutureConfigurationsSurviveStorageWithoutBecomingUsable() = runBlocking<Unit> {
        val repository = LocalProviderConnectionsRepository(database.providerConnectionsDao())
        listOf("not json", """{"apiToken":"secret","version":999}"""").forEach { raw ->
            val configuration = ProviderConfigurationJson.decode(ApiProviderType.Apify, raw)
            assertIs<InvalidProviderConfiguration>(configuration)
            val id = repository.save(
                ProviderConnection(
                    type = ApiProviderType.Apify,
                    name = "Repair",
                    configuration = configuration,
                ),
            )
            val saved = repository.get(id)!!
            assertFalse(saved.usable)
            assertEquals(raw, ProviderConfigurationJson.encode(saved.configuration))
            assertFalse("secret" in saved.configuration.toString())
        }
    }
}
