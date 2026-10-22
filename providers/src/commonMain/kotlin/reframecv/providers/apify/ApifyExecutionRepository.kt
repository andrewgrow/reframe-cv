package reframecv.providers.apify

import io.ktor.client.HttpClient
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.LinkedInJobSearchConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.network.HttpTrafficLogger
import reframecv.runners.RunnerExecutionError
import reframecv.runners.RunnerExecutionException
import reframecv.runners.RunnerExecutionRepository

/** Adapts the supported LinkedIn Actor to the application's remote execution contract. */
class ApifyExecutionRepository(
    client: HttpClient,
    logger: HttpTrafficLogger = HttpTrafficLogger.None,
) : RunnerExecutionRepository {
    private val http = ApifyHttp(client, logger)
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun start(
        connection: ProviderConnection,
        configuration: LinkedInJobSearchConfiguration,
    ) = decode {
        require(configuration.valid)
        val body = json.encodeToString(
            LinkedInJobsInput(configuration.searchUrls, configuration.maxResults),
        )
        val (text) = http.request(
            "actors/curious_coder~linkedin-jobs-scraper/runs",
            token(connection),
            body,
        )
        json.decodeFromString<ApifyRunResponse>(text).data.toDomain()
    }
    override suspend fun inspect(connection: ProviderConnection, externalId: String) = decode {
        require(externalId.matches(Regex("[a-zA-Z0-9]+")))
        val (text) = http.request("actor-runs/$externalId", token(connection))
        json.decodeFromString<ApifyRunResponse>(text).data.toDomain()
    }
    override suspend fun results(
        connection: ProviderConnection,
        datasetId: String,
        offset: Int,
        limit: Int,
    ) = decode {
        require(datasetId.matches(Regex("[a-zA-Z0-9]+")))
        require(offset >= 0 && limit in 1..MAX_PAGE_SIZE)
        val (text, total) = http.request(
            "datasets/$datasetId/items?format=json&offset=$offset&limit=$limit",
            token(connection),
        )
        mapApifyPage(json, text, offset, limit, total)
    }
    private fun token(connection: ProviderConnection): String {
        if (!connection.active || !connection.usable || connection.type != ApiProviderType.Apify) {
            throw RunnerExecutionException(
                RunnerExecutionError.Disabled,
                "Apify connection is disabled or not configured.",
            )
        }
        return (connection.configuration as ApifyConfiguration).apiToken.trim()
    }
    private suspend fun <T> decode(block: suspend () -> T): T = try {
        block()
    } catch (_: IllegalArgumentException) {
        invalidResponse()
    } catch (error: IllegalStateException) {
        if (error is CancellationException) throw error
        invalidResponse()
    }
    private fun invalidResponse(): Nothing = throw RunnerExecutionException(
        RunnerExecutionError.InvalidResponse,
        "Invalid Apify response or operation settings.",
    )
}
private const val MAX_PAGE_SIZE = 100
