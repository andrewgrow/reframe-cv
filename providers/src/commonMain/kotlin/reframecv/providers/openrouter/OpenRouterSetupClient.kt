package reframecv.providers.openrouter

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import reframecv.domain.models.providers.AiModel
import reframecv.network.HttpTrafficLog
import reframecv.network.HttpTrafficLogger
import reframecv.providers.ProviderAccountClient
import reframecv.providers.ProviderSetupClient
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

/** Loads OpenRouter models, key budgets, and optional management-account credits. */
class OpenRouterSetupClient(
    private val transport: HttpClient,
    logger: HttpTrafficLogger = HttpTrafficLogger.None,
) : ProviderSetupClient,
    ProviderAccountClient {
    private val traffic = HttpTrafficLog(logger)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadModels(apiKey: String): List<AiModel> = call {
        val key = json.parseToJsonElement(request("key", apiKey)).jsonObject
        checkNotNull(key["data"]).jsonObject
        val response = json.decodeFromString<Catalog>(request("models/user", apiKey))
        response.data.filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.id }.sortedBy { it.name.lowercase() }
            .map { AiModel(it.id, it.name, "structured_outputs" in it.parameters) }
    }

    override suspend fun accountInfo(apiKey: String) = call {
        json.decodeFromString<OpenRouterAccountResponse>(request("key", apiKey)).data.toDomain()
    }

    override suspend fun balance(managementApiKey: String) = call {
        json.decodeFromString<OpenRouterCreditsResponse>(
            request("credits", managementApiKey),
        ).data.toDomain()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: ProviderSetupException) {
        throw error
    } catch (_: IllegalArgumentException) {
        throw ProviderSetupException(ProviderSetupError.InvalidResponse)
    } catch (_: IllegalStateException) {
        throw ProviderSetupException(ProviderSetupError.InvalidResponse)
    } catch (_: Exception) {
        // Do not propagate Ktor exceptions: requests may contain authentication headers.
        throw ProviderSetupException(ProviderSetupError.Unavailable)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun request(path: String, key: String): String {
        val url = "https://openrouter.ai/api/v1/$path"
        val secrets = listOf(key)
        traffic.request("GET", url, mapOf("Authorization" to "Bearer $key"), secrets)
        val (response, body) = try {
            val response = transport.get(url) { bearerAuth(key) }
            response to response.bodyAsText()
        } catch (error: Exception) {
            if (error !is CancellationException) traffic.failure("GET", url, error)
            throw error
        }
        traffic.response(
            "GET",
            url,
            response.status.value,
            response.headers.entries().associate { it.key to it.value.joinToString(", ") },
            body,
            secrets,
        )
        if (!response.status.isSuccess()) {
            throw ProviderSetupException(
                if (response.status == HttpStatusCode.Unauthorized ||
                    response.status == HttpStatusCode.Forbidden
                ) {
                    ProviderSetupError.Unauthorized
                } else {
                    ProviderSetupError.Unavailable
                },
            )
        }
        return body
    }

    /** Reads only the model fields used in settings. */
    @Serializable
    private data class Catalog(val data: List<Model>)

    /** Keeps transport-specific catalog properties out of application models. */
    @Serializable
    private data class Model(
        val id: String,
        val name: String,
        @SerialName("supported_parameters") val parameters: List<String> = emptyList(),
    )
}
