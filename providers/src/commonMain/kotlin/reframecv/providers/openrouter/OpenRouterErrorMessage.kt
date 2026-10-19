package reframecv.providers.openrouter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Extracts provider feedback without exposing credentials or raw HTTP responses. */
internal fun openRouterErrorMessage(content: String, key: String): String? {
    val response = runCatching { Json.parseToJsonElement(content) as? JsonObject }.getOrNull()
    val error = response?.get("error") as? JsonObject
    val message = (error?.get("message") as? JsonPrimitive)?.takeIf { it.isString }
        ?.content?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (key.isNotEmpty()) message.replace(key, "[REDACTED]") else message
}
