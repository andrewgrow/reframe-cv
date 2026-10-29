package reframecv.providers.openai

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenAiConfiguration
import reframecv.network.HttpTrafficLog
import reframecv.network.HttpTrafficLogger
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse

/** Sends shared AI operations directly to OpenAI without retaining responses on the API. */
class OpenAiClient(
    private val transport: HttpClient,
    logger: HttpTrafficLogger = HttpTrafficLogger.None,
) : AiProviderClient {
    private val traffic = HttpTrafficLog(logger)

    override suspend fun execute(request: AiRequest): AiResponse =
        withContext(Dispatchers.Default) {
            val connection = request.connection
            val config = connection.configuration as? OpenAiConfiguration
            val ready =
                connection.active && connection.type == ApiProviderType.OpenAI && connection.usable
            if (!ready || config == null || config.modelId.isBlank()) {
                AiResponse.Failed(AiProviderError.NotConfigured)
            } else {
                generate(request, config.apiKey.trim())
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun generate(request: AiRequest, key: String): AiResponse = try {
        val body = openAiRequestBody(request).toString()
        traffic.request("POST", URL, mapOf("Authorization" to "Bearer $key"), listOf(key), body)
        val response = transport.post(URL) {
            bearerAuth(key)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val content = response.bodyAsText()
        traffic.response(
            "POST",
            URL,
            response.status.value,
            response.headers.entries().associate { it.key to it.value.joinToString(", ") },
            content,
            listOf(key),
        )
        decodeOpenAiResponse(content, response.status.value, key)
    } catch (error: CancellationException) {
        throw error
    } catch (_: IllegalArgumentException) {
        AiResponse.Failed(AiProviderError.InvalidResponse)
    } catch (error: Exception) {
        traffic.failure("POST", URL, error)
        AiResponse.Failed(AiProviderError.Unavailable)
    }

    override fun close() {
        transport.close()
        transport.engine.close()
    }

    companion object {
        private const val URL = "https://api.openai.com/v1/responses"
    }
}
