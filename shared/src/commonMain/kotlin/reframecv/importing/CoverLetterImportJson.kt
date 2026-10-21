package reframecv.importing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import reframecv.domain.models.coverletter.CoverLetter

/** Reads recognized portable fields, ignoring unsupported fields and malformed field values. */
internal object CoverLetterImportJson {
    fun decode(value: String): CoverLetter {
        val document = Json.parseToJsonElement(value.removePrefix("\uFEFF")) as? JsonObject
        require(document != null) { "Cover letter document must be a JSON object" }
        val version = document["formatVersion"] as? JsonPrimitive
        require(version != null && !version.isString && version.intOrNull == 1) {
            "Unsupported cover letter document version"
        }
        val content = document["content"] as? JsonObject
        require(content != null) { "Cover letter document must contain a content object" }
        return CoverLetter(
            projectId = 0,
            createdAt = 0,
            updatedAt = 0,
            name = content.text("name"),
            content = content.text("text"),
            keywords = (content["keywords"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content
            }.orEmpty(),
        )
    }
    private fun JsonObject.text(name: String): String =
        (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
}
