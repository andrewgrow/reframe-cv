package reframecv.providers.openrouter

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.network.HttpTrafficLog
import reframecv.network.HttpTrafficLogger
import reframecv.providers.AiProviderClient
import reframecv.providers.AiProviderError
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse

/** Sends source documents and a structured-output contract to the selected OpenRouter model. */
class OpenRouterClient(
    private val transport: HttpClient,
    logger: HttpTrafficLogger = HttpTrafficLogger.None,
) : AiProviderClient {
    private val traffic = HttpTrafficLog(logger)

    override suspend fun execute(request: AiRequest): AiResponse =
        withContext(Dispatchers.Default) {
            generate(request)
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun generate(request: AiRequest): AiResponse {
        val connection = request.connection
        val credentialsReady = connection.configuration is OpenRouterConfiguration &&
            connection.usable && connection.openRouter.modelId.isNotBlank()
        if (!connection.active || connection.type != ApiProviderType.OpenRouter ||
            !credentialsReady
        ) {
            return AiResponse.Failed(AiProviderError.NotConfigured)
        }
        val key = connection.openRouter.apiKey.trim()
        return try {
            val body = requestBody(request).toString()
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
            decodeGeneration(content, response.status.value, key)
        } catch (error: CancellationException) {
            throw error
        } catch (_: IllegalArgumentException) {
            AiResponse.Failed(AiProviderError.InvalidResponse)
        } catch (_: NoSuchElementException) {
            AiResponse.Failed(AiProviderError.InvalidResponse)
        } catch (error: Exception) {
            traffic.failure("POST", URL, error)
            AiResponse.Failed(AiProviderError.Unavailable)
        }
    }

    override fun close() {
        transport.close()
        transport.engine.close()
    }

    companion object {
        private const val URL = "https://openrouter.ai/api/v1/chat/completions"
    }
}

private fun requestBody(request: AiRequest) = buildJsonObject {
    put("model", request.connection.openRouter.modelId)
    put("stream", false)
    putJsonObject("provider") { put("require_parameters", true) }
    putJsonObject("response_format") {
        put("type", "json_schema")
        putJsonObject("json_schema") {
            put("name", "structured_result")
            put("strict", true)
            put("schema", Json.parseToJsonElement(request.responseSchema))
        }
    }
    putJsonArray("messages") {
        addJsonObject {
            put("role", "user")
            putJsonArray("content") {
                addJsonObject {
                    put("type", "text")
                    put("text", request.instruction)
                }
                request.textDocuments.forEach { document ->
                    addJsonObject {
                        put("type", "text")
                        put("text", "${document.name}\n${document.content}")
                    }
                }
                request.documents.forEach { document ->
                    addJsonObject {
                        put("type", "file")
                        putJsonObject("file") {
                            put("filename", document.fileName)
                            put(
                                "file_data",
                                "data:${document.mediaType};base64,${Base64.encode(
                                    document.bytes,
                                )}",
                            )
                        }
                    }
                }
            }
        }
    }
}
