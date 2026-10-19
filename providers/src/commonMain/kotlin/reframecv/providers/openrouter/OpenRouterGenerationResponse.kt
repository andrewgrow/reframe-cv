package reframecv.providers.openrouter

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import reframecv.providers.AiProviderError
import reframecv.providers.AiResponse

/** Decodes provider envelopes separately from the operation's structured result. */
internal fun decodeGeneration(content: String, status: Int, key: String): AiResponse =
    if (!HttpStatusCode.fromValue(status).isSuccess()) {
        AiResponse.Failed(
            AiProviderError.Unavailable,
            openRouterErrorMessage(content, key) ?: "HTTP $status",
        )
    } else {
        val result = Json.parseToJsonElement(content).jsonObject
        if (result["error"] != null) {
            AiResponse.Failed(AiProviderError.Unavailable, openRouterErrorMessage(content, key))
        } else {
            decodeChoice(result)
        }
    }

private fun decodeChoice(result: JsonObject): AiResponse {
    val choice = result.getValue("choices").jsonArray.first().jsonObject
    val finish = choice["finish_reason"]?.jsonPrimitive?.contentOrNull
    val message = choice.getValue("message").jsonObject
    val text = message["content"]?.jsonPrimitive?.contentOrNull
    val stopped = finish == null || finish == "stop"
    val refused = message["refusal"]?.jsonPrimitive?.contentOrNull != null
    return if (text.isNullOrBlank() || !stopped || refused) {
        AiResponse.Failed(AiProviderError.InvalidResponse)
    } else {
        AiResponse.Generated(text)
    }
}
