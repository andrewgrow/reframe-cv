package reframecv.providers.openai

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import reframecv.domain.models.providers.AiModel
import reframecv.network.HttpTrafficLog
import reframecv.network.HttpTrafficLogger
import reframecv.providers.ProviderSetupClient
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

/** Discovers accessible model IDs; the catalog does not advertise structured-output capabilities. */
class OpenAiSetupClient(
    private val transport: HttpClient,
    logger: HttpTrafficLogger = HttpTrafficLogger.None,
) : ProviderSetupClient {
    private val traffic = HttpTrafficLog(logger)
    private val json = Json { ignoreUnknownKeys = true }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun loadModels(apiKey: String): List<AiModel> = try {
        val key = apiKey.trim()
        traffic.request("GET", URL, mapOf("Authorization" to "Bearer $key"), listOf(key))
        val response = transport.get(URL) { bearerAuth(key) }
        val body = response.bodyAsText()
        traffic.response(
            "GET",
            URL,
            response.status.value,
            response.headers.entries().associate { it.key to it.value.joinToString(", ") },
            body,
            listOf(key),
        )
        if (!response.status.isSuccess()) {
            throw ProviderSetupException(
                if (response.status in
                    listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)
                ) {
                    ProviderSetupError.Unauthorized
                } else {
                    ProviderSetupError.Unavailable
                },
                providerMessage = openAiErrorMessage(body, key),
            )
        }
        json.decodeFromString<Catalog>(body).data.filter { it.id.isNotBlank() }
            .distinctBy { it.id }.sortedBy { it.id.lowercase() }.map { AiModel(it.id, it.id) }
    } catch (error: CancellationException) {
        throw error
    } catch (error: ProviderSetupException) {
        throw error
    } catch (_: IllegalArgumentException) {
        throw ProviderSetupException(ProviderSetupError.InvalidResponse)
    } catch (error: Exception) {
        traffic.failure("GET", URL, error)
        throw ProviderSetupException(ProviderSetupError.Unavailable)
    }

    @Serializable
    private data class Catalog(val data: List<Model>)

    @Serializable
    private data class Model(val id: String)

    companion object {
        private const val URL = "https://api.openai.com/v1/models"
    }
}
