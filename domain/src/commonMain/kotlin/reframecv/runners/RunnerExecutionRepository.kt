package reframecv.runners

import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.LinkedInJobSearchConfiguration
import reframecv.domain.models.providers.ProviderConnection

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
        configuration: LinkedInJobSearchConfiguration,
    ): RemoteRunnerRun
    suspend fun inspect(connection: ProviderConnection, externalId: String): RemoteRunnerRun
    suspend fun results(
        connection: ProviderConnection,
        datasetId: String,
        offset: Int,
        limit: Int,
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
