package reframecv.importing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import reframecv.domain.models.providers.normalized
import reframecv.domain.models.resume.content.ResumeContent

/** Reads recognized fields from imported resume documents without weakening database decoding. */
internal object ResumeImportJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(value: String): ResumeContent {
        val document = json.parseToJsonElement(value.removePrefix("\uFEFF")) as? JsonObject
        require(document != null) { "Resume document must be a JSON object" }
        val version = (document["formatVersion"] as? JsonPrimitive)?.intOrNull
        require(version == 1) { "Unsupported resume document version: $version" }
        val content = document["content"] as? JsonObject
        require(content != null) { "Resume document must contain a content object" }
        val normalized = if ("consent" !in content) {
            val legacy = buildMap {
                content["consentStatement"]?.let { put("text", it) }
                content["consentStatementVisible"]?.let { put("visible", it) }
            }
            JsonObject(content + ("consent" to JsonObject(legacy)))
        } else {
            content
        }
        val recognized = ResumeImportSanitizer.sanitize(
            normalized,
            ResumeContent.serializer().descriptor,
        )
        return json.decodeFromJsonElement(ResumeContent.serializer(), checkNotNull(recognized))
    }
}
