package reframecv.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import reframecv.domain.models.resume.content.ResumeContent

/** Encodes resume documents for storage and rejects unsupported or invalid formats. */
object ResumeContentJson {
    private const val FORMAT_VERSION = 1
    private val json = Json { encodeDefaults = true }
    private val exportJson = Json(json) { prettyPrint = true }

    fun encode(content: ResumeContent): String =
        json.encodeToString(Document.serializer(), Document(FORMAT_VERSION, content))

    fun encodeForExport(content: ResumeContent): String =
        exportJson.encodeToString(Document.serializer(), Document(FORMAT_VERSION, content)) + "\n"

    fun decodeGenerated(value: String): ResumeContent {
        val input = json.parseToJsonElement(value)
        val document = json.decodeFromJsonElement(Document.serializer(), input)
        require(document.formatVersion == FORMAT_VERSION) {
            "Unsupported resume content format version"
        }
        requireUnchangedValues(input, json.encodeToJsonElement(Document.serializer(), document))
        return document.content
    }

    private fun requireUnchangedValues(input: JsonElement, decoded: JsonElement) {
        when (input) {
            is JsonObject -> {
                require(decoded is JsonObject)
                input.forEach { (key, value) ->
                    requireUnchangedValues(value, decoded.getValue(key))
                }
            }

            is JsonArray -> {
                require(decoded is JsonArray && input.size == decoded.size)
                input.forEachIndexed { index, value ->
                    requireUnchangedValues(value, decoded[index])
                }
            }

            else -> require(input == decoded) {
                "Generated resume fields must use their declared JSON types"
            }
        }
    }

    fun decode(value: String): ResumeContent {
        val document = json.parseToJsonElement(value).jsonObject
        val version = document["formatVersion"]?.jsonPrimitive?.intOrNull
        require(version == FORMAT_VERSION) { "Unsupported resume content format version: $version" }
        return json.decodeFromJsonElement(Document.serializer(), normalizeConsent(document)).content
    }

    private fun normalizeConsent(document: JsonObject): JsonObject {
        val content = document["content"]?.jsonObject
        val legacyFields = listOf("consentStatement", "consentStatementVisible")
        if (content == null || legacyFields.none { it in content }) return document
        require("consent" !in content) { "Resume content mixes legacy and nested consent fields" }
        val consent = buildMap {
            content["consentStatement"]?.let { put("text", it) }
            content["consentStatementVisible"]?.let { put("visible", it) }
        }
        val normalized = content.toMutableMap().apply {
            legacyFields.forEach { remove(it) }
            put("consent", JsonObject(consent))
        }
        return JsonObject(document + ("content" to JsonObject(normalized)))
    }

    /** Wraps persisted resume content with its independent format version. */
    @Serializable
    private data class Document(val formatVersion: Int, val content: ResumeContent)
}
