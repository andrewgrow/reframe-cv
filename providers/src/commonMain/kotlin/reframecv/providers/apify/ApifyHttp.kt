package reframecv.providers.apify

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import reframecv.network.HttpTrafficLog
import reframecv.network.HttpTrafficLogger
import reframecv.runners.RunnerExecutionError
import reframecv.runners.RunnerExecutionException

/** Applies bearer authentication and credential-redacted diagnostics to Apify exchanges. */
internal class ApifyHttp(private val client: HttpClient, logger: HttpTrafficLogger) {
    private val traffic = HttpTrafficLog(logger)
    private val json = Json { ignoreUnknownKeys = true }

    @Suppress("TooGenericExceptionCaught")
    suspend fun request(path: String, key: String, body: String? = null): Pair<String, Int?> {
        val method = if (body == null) HttpMethod.Get else HttpMethod.Post
        val url = "https://api.apify.com/v2/$path"
        val secrets = listOf(key)
        traffic.request(method.value, url, mapOf("Authorization" to "Bearer $key"), secrets, body)
        val response = execute(url, method, key, body)
        val text = readBody(response)
        traffic.response(
            method.value,
            url,
            response.status.value,
            response.headers.entries().associate {
                it.key to it.value.joinToString(", ")
            },
            text,
            secrets,
        )
        if (!response.status.isSuccess()) {
            val reason = if (response.status.value in
                setOf(401, 403)
            ) {
                RunnerExecutionError.Unauthorized
            } else {
                RunnerExecutionError.Unavailable
            }
            throw RunnerExecutionException(reason, errorMessage(text, key, response.status.value))
        }
        return text to
            response.headers["X-Apify-Pagination-Total"]?.toIntOrNull()?.takeIf { it >= 0 }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun execute(
        url: String,
        method: HttpMethod,
        key: String,
        body: String?,
    ): HttpResponse = try {
        client.request(url) {
            this.method = method
            bearerAuth(key)
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        traffic.failure(method.value, url, error)
        throw RunnerExecutionException(
            RunnerExecutionError.Unavailable,
            "Could not contact Apify.",
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun readBody(response: HttpResponse): String = try {
        response.bodyAsText()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        throw RunnerExecutionException(
            RunnerExecutionError.Unavailable,
            "Could not read Apify response.",
        )
    }
    private fun errorMessage(body: String, key: String, status: Int): String {
        val message = try {
            val error = json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
            (error?.get("message") as? JsonPrimitive)?.content
        } catch (_: IllegalArgumentException) {
            null
        }
        return message?.replace(key, "[REDACTED]")?.take(MAX_ERROR_LENGTH)
            ?.takeIf { it.isNotBlank() } ?: "Apify request failed (HTTP $status)."
    }
}
private const val MAX_ERROR_LENGTH = 500
