package reframecv.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.repository.ConnectionRunnersRepository
import reframecv.repository.ProviderConnectionsRepository

/** Identifies a saved runner together with its owning connection and availability. */
data class SavedRunner(val runner: ConnectionRunner, val connection: ProviderConnection) {
    val available: Boolean get() = runner.active && connection.active && connection.usable &&
        RunnerConfigurationJson.decode(runner) != null
}

/** Validates saved operations and snapshots their inputs before an execution starts. */
class ConnectionRunnersUseCases(
    private val runners: ConnectionRunnersRepository,
    private val connections: ProviderConnectionsRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSaved() = connections.observe().flatMapLatest { owners ->
        if (owners.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(owners.map { runners.observe(it.id) }) { groups ->
                groups.flatMapIndexed { index, group ->
                    group.map { SavedRunner(it, owners[index]) }
                }
                    .sortedBy { it.runner.id }
            }
        }
    }
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
