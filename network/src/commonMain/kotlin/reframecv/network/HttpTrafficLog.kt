package reframecv.network

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Receives HTTP diagnostics independently of platform file access. */
fun interface HttpTrafficLogger {
    suspend fun write(message: String)

    companion object {
        val None = HttpTrafficLogger { }
    }
}

/** Formats complete text exchanges while removing credentials from headers and JSON. */
class HttpTrafficLog(private val logger: HttpTrafficLogger) {
    private val json = Json { prettyPrint = true }

    suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        secrets: List<String>,
        body: String? = null,
    ) = write("REQUEST $method $url", headers, body, secrets)

    suspend fun response(
        method: String,
        url: String,
        status: Int,
        headers: Map<String, String>,
        body: String,
        secrets: List<String>,
    ) = write("RESPONSE $status $method $url", headers, body, secrets)

    suspend fun failure(method: String, url: String, error: Throwable) =
        emit("FAILURE $method $url: ${error::class.simpleName}")

    private suspend fun write(
        title: String,
        headers: Map<String, String>,
        body: String?,
        secrets: List<String>,
    ) {
        if (logger === HttpTrafficLogger.None) return
        val message = buildString {
            appendLine(title)
            headers.forEach { (name, value) ->
                appendLine("$name: ${if (sensitive(name)) REDACTED else value}")
            }
            if (body != null) {
                appendLine()
                appendLine(redactBody(body))
            }
        }
        emit(
            secrets.filter(String::isNotEmpty).fold(message) { value, secret ->
                value.replace(secret, REDACTED)
            },
        )
    }

    private fun redactBody(body: String): String = try {
        json.encodeToString(JsonElement.serializer(), redact(json.parseToJsonElement(body)))
    } catch (_: IllegalArgumentException) {
        body
    }

    private fun redact(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject(
            value.mapValues { (key, content) ->
                if (sensitive(key)) JsonPrimitive(REDACTED) else redact(content)
            },
        )

        is JsonArray -> JsonArray(value.map(::redact))

        else -> value
    }

    private fun sensitive(name: String) = name.lowercase().filter(Char::isLetterOrDigit) in setOf(
        "authorization", "proxyauthorization", "cookie", "setcookie", "xapikey",
        "filedata", "apikey", "key", "token", "accesstoken", "refreshtoken", "password", "secret",
    )

    @Suppress("TooGenericExceptionCaught")
    private suspend fun emit(message: String) {
        try {
            logger.write(message)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Diagnostic failures must not change the API result.
        }
    }

    companion object {
        private const val REDACTED = "[REDACTED]"
    }
}
