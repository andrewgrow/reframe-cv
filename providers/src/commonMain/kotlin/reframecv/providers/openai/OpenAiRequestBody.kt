package reframecv.providers.openai

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import reframecv.providers.AiRequest

/** Converts shared prompts, JSON documents and attachments into a Responses API request. */
internal fun openAiRequestBody(request: AiRequest) = buildJsonObject {
    put("model", request.connection.ai.modelId)
    put("store", false)
    put("stream", false)
    putJsonObject("text") {
        putJsonObject("format") {
            put("type", "json_schema")
            put("name", "structured_result")
            put("strict", true)
            put("schema", Json.parseToJsonElement(request.responseSchema))
        }
    }
    putJsonArray("input") {
        addJsonObject {
            put("role", "user")
            putJsonArray("content") {
                addJsonObject {
                    put("type", "input_text")
                    put("text", request.instruction)
                }
                request.textDocuments.forEach { document ->
                    addJsonObject {
                        put("type", "input_text")
                        put("text", "${document.name}\n${document.content}")
                    }
                }
                request.documents.forEach { document ->
                    addJsonObject {
                        val data = "data:${document.mediaType};base64,${Base64.encode(
                            document.bytes,
                        )}"
                        if (document.mediaType.startsWith("image/")) {
                            put("type", "input_image")
                            put("image_url", data)
                        } else {
                            put("type", "input_file")
                            put("filename", document.fileName)
                            put("file_data", data)
                        }
                    }
                }
            }
        }
    }
}
