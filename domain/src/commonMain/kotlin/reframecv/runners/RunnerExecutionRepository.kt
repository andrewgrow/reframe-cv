package reframecv.runners

import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerConfiguration

/** Describes a provider execution without exposing HTTP response types to application features. */
data class RemoteRunnerRun(
    val id: String,
    val state: ConnectionRunState,
    val datasetId: String? = null,
    val finishedAt: Long? = null,
    val details: String? = null,
    val costUsd: Double? = null,
)

/** Provides remote execution separately from local runner definitions and history. */
interface RunnerExecutionRepository {
    suspend fun start(
        connection: ProviderConnection,
        configuration: RunnerConfiguration,
    ): RemoteRunnerRun
    suspend fun inspect(connection: ProviderConnection, externalId: String): RemoteRunnerRun
    suspend fun results(
        connection: ProviderConnection,
        datasetId: String,
        offset: Int,
        limit: Int,
        operation: ConnectionOperation = ConnectionOperation.LinkedInJobSearch,
    ): RunnerResultsPage
}

/** Categorizes safe failures while preserving provider messages that contain no credentials. */
enum class RunnerExecutionError {
    Disabled,
    Configuration,
    Unauthorized,
    Unavailable,
    InvalidResponse,
}

/** Prevents transport exceptions containing credentials from escaping the provider layer. */
class RunnerExecutionException(val reason: RunnerExecutionError, val details: String) :
    Exception(details)
