package reframecv.usecase

import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.repository.ConnectionRunnersRepository
import reframecv.repository.ProviderConnectionsRepository

/** Validates saved operations and snapshots their inputs before an execution starts. */
class ConnectionRunnersUseCases(
    private val runners: ConnectionRunnersRepository,
    private val connections: ProviderConnectionsRepository,
) {
    fun observe(connectionId: Long) = runners.observe(connectionId)
    fun runs(runnerId: Long) = runners.runs(runnerId)
    suspend fun save(runner: ConnectionRunner): Long {
        val connection = checkNotNull(connections.get(runner.connectionId))
        require(connection.type == ApiProviderType.Apify)
        require(RunnerConfigurationJson.decode(runner) != null)
        return runners.save(runner.copy(name = runner.name.trim()))
    }
    suspend fun delete(id: Long) = runners.delete(id)
    suspend fun prepareRun(runnerId: Long, timestamp: Long): Long {
        val runner = checkNotNull(runners.get(runnerId))
        val connection = checkNotNull(connections.get(runner.connectionId))
        require(runner.active && connection.active && connection.usable)
        require(connection.type == ApiProviderType.Apify)
        val configuration = checkNotNull(RunnerConfigurationJson.decode(runner))
        return runners.createRun(
            ConnectionRun(
                runnerId = runner.id,
                configurationSnapshot = RunnerConfigurationJson.encode(configuration),
                startedAt = timestamp,
            ),
        )
    }
    suspend fun updateRun(run: ConnectionRun) = runners.updateRun(run)
}
