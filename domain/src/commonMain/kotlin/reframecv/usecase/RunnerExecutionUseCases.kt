package reframecv.usecase

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.repository.ConnectionRunnersRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.RunnerResultsRepository
import reframecv.runners.RemoteRunnerRun
import reframecv.runners.RunnerExecutionError
import reframecv.runners.RunnerExecutionException
import reframecv.runners.RunnerExecutionRepository
import reframecv.runners.RunnerResultsPage
import reframecv.shared.nowMillis

/** Combines persisted run identity and the current provider response for result review. */
data class RunnerRunView(
    val run: ConnectionRun,
    val remote: RemoteRunnerRun?,
    val page: RunnerResultsPage? = null,
)

/** Starts once, persists external identities, and resumes reads without repeating paid requests. */
class RunnerExecutionUseCases(
    private val runners: ConnectionRunnersRepository,
    private val connections: ProviderConnectionsRepository,
    private val remote: RunnerExecutionRepository,
    private val results: RunnerResultsRepository,
    private val now: () -> Long = ::nowMillis,
) {
    val dataset = RunnerDatasetUseCases(runners, connections, remote, results, now)
    fun observe(runnerId: Long) = runners.runs(runnerId)

    @Suppress("TooGenericExceptionCaught")
    suspend fun start(runnerId: Long): ConnectionRun {
        val runner = checkNotNull(runners.get(runnerId))
        val connection = connection(runner.connectionId)
        val config = configuration(runner)
        val pending =
            ConnectionRun(
                runnerId = runner.id,
                configurationSnapshot = RunnerConfigurationJson.encode(config),
                startedAt = now(),
            )
        val local = pending.copy(id = runners.createRun(pending))
        val result = try {
            remote.start(connection, config)
        } catch (error: Exception) {
            withContext(NonCancellable) {
                runners.updateRun(
                    local.copy(
                        state = ConnectionRunState.Failed,
                        finishedAt = now(),
                        error = "Could not confirm the start. " +
                            "Check Apify Console before starting another run.",
                    ),
                )
            }
            throw error
        }
        return persistStarted(local, result)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun persistStarted(local: ConnectionRun, result: RemoteRunnerRun) = try {
        withContext(NonCancellable) { persist(local, result) }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        throw RunnerExecutionException(
            RunnerExecutionError.Unavailable,
            "Apify run ${result.id} started, but its ID could not be saved. " +
                "Check Apify Console before starting again.",
        )
    }
    private fun configuration(
        runner: reframecv.domain.models.providers.ConnectionRunner,
    ): reframecv.domain.models.providers.LinkedInJobSearchConfiguration {
        if (!runner.active) {
            throw RunnerExecutionException(
                RunnerExecutionError.Disabled,
                "Runner is disabled.",
            )
        }
        return RunnerConfigurationJson.decode(
            runner,
        ) ?: throw RunnerExecutionException(
            RunnerExecutionError.Configuration,
            "Invalid runner configuration.",
        )
    }

    suspend fun inspect(
        runnerId: Long,
        runId: Long,
        offset: Int = 0,
        filter: RunnerResultFilter = RunnerResultFilter(),
    ): RunnerRunView {
        require(offset >= 0)
        val local = checkNotNull(runners.getRun(runId))
        require(local.runnerId == runnerId)
        val runner = checkNotNull(runners.get(runnerId))
        val connection = connection(runner.connectionId)
        val externalId = local.externalId ?: return RunnerRunView(local, null)
        val status = remote.inspect(connection, externalId)
        require(status.id == externalId) { "Unexpected run identity" }
        val saved = persist(local, status)
        return if (saved.state == ConnectionRunState.Succeeded) {
            if (saved.datasetId == null) {
                throw RunnerExecutionException(
                    RunnerExecutionError.InvalidResponse,
                    "Apify did not return a dataset ID.",
                )
            }
            val view = if (saved.nextResultOffset == 0 && !saved.resultsComplete) {
                dataset.loadMore(runnerId, runId, offset, filter)
            } else {
                dataset.page(runnerId, runId, offset, filter)
            }
            view.copy(remote = status)
        } else {
            RunnerRunView(saved, status)
        }
    }
    private suspend fun persist(local: ConnectionRun, status: RemoteRunnerRun): ConnectionRun {
        val saved = local.copy(
            externalId = status.id,
            datasetId = status.datasetId ?: local.datasetId,
            costUsd = status.costUsd ?: local.costUsd,
            state = status.state,
            finishedAt = status.finishedAt,
            error = status.details.takeIf {
                status.state == ConnectionRunState.Failed ||
                    status.state == ConnectionRunState.Aborted
            },
        )
        runners.updateRun(saved)
        return saved
    }
    private suspend fun connection(id: Long): ProviderConnection {
        val connection = connections.get(id)
        if (connection == null || !connection.active || !connection.usable) {
            throw RunnerExecutionException(
                RunnerExecutionError.Disabled,
                "Provider connection is disabled or not configured.",
            )
        }
        if (connection.type != ApiProviderType.Apify) {
            throw RunnerExecutionException(
                RunnerExecutionError.Configuration,
                "This operation requires an Apify connection.",
            )
        }
        return connection
    }
}
