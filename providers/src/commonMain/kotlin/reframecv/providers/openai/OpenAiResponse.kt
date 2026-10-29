package reframecv.providers.openai

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import reframecv.providers.AiProviderError
import reframecv.providers.AiResponse

/** Reads only structured provider feedback and removes the credential from user-visible errors. */
internal fun openAiErrorMessage(content: String, key: String): String? {
    val result = runCatching { Json.parseToJsonElement(content) as? JsonObject }.getOrNull()
    val error = result?.get("error") as? JsonObject
    val message = error?.text("message")?.trim()?.takeIf(String::isNotEmpty)
    return message?.let { if (key.isEmpty()) it else it.replace(key, "[REDACTED]") }
}

/** Ignores reasoning items and rejects incomplete, refused or ambiguous output envelopes. */
internal fun decodeOpenAiResponse(content: String, status: Int, key: String): AiResponse {
    val error = openAiErrorMessage(content, key)
    if (!HttpStatusCode.fromValue(status).isSuccess() || error != null) {
        return AiResponse.Failed(AiProviderError.Unavailable, error ?: "HTTP $status")
    }
    val result = Json.parseToJsonElement(content) as? JsonObject
    val output = result?.get("output") as? JsonArray
    val messages = output?.mapNotNull { it as? JsonObject }?.filter { it.text("type") == "message" }
    val message = messages?.singleOrNull()
    val parts = message?.get("content") as? JsonArray
    val text = parts?.singleOrNull() as? JsonObject
    val completed = result?.text("status") == "completed" &&
        message?.text("status") == "completed"
    val generated = text?.takeIf { it.text("type") == "output_text" }?.text("text")
    return if (completed && !generated.isNullOrBlank()) {
        AiResponse.Generated(generated)
    } else {
        AiResponse.Failed(AiProviderError.InvalidResponse)
    }
}

private fun JsonObject.text(key: String) =
    (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
