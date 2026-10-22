package reframecv.repository

import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.LinkedInJobSearchConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.runners.RemoteRunnerRun
import reframecv.runners.RunnerExecutionRepository
import reframecv.runners.RunnerResultsPage

class TestRunnerExecutionRepository : RunnerExecutionRepository {
    var starts = 0
    var reads = 0
    var resultError: Exception? = null
    var resultReads = 0
    var requestedOffset = 0
    var readError: Exception? = null
    var beforeRead: suspend () -> Unit = {}
    var status = RemoteRunnerRun("remoteRun1", ConnectionRunState.Succeeded, "dataset1", 123)
    var page = RunnerResultsPage(emptyList(), 0, null, 0)
    var startError: Exception? = null
    var beforeStart: suspend () -> Unit = {}
    override suspend fun start(
        connection: ProviderConnection,
        configuration: LinkedInJobSearchConfiguration,
    ): RemoteRunnerRun {
        starts++
        beforeStart()
        startError?.let { throw it }
        return status
    }
    override suspend fun inspect(
        connection: ProviderConnection,
        externalId: String,
    ): RemoteRunnerRun {
        reads++
        beforeRead()
        readError?.let { throw it }
        return status.copy(id = externalId)
    }
    override suspend fun results(
        connection: ProviderConnection,
        datasetId: String,
        offset: Int,
        limit: Int,
    ): RunnerResultsPage {
        resultReads++
        requestedOffset = offset
        resultError?.let { throw it }
        return page.copy(offset = offset)
    }
}
