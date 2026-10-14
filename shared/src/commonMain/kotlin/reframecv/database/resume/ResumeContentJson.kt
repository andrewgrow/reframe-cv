package reframecv.database.resume

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import reframecv.domain.models.resume.content.ResumeContent

/** Encodes resume documents for storage and rejects unsupported or invalid formats. */
internal object ResumeContentJson {
    private const val FORMAT_VERSION = 1
    private val json = Json { encodeDefaults = true }
    private val exportJson = Json(json) { prettyPrint = true }

    fun encode(content: ResumeContent): String =
        json.encodeToString(Document.serializer(), Document(FORMAT_VERSION, content))

    fun encodeForExport(content: ResumeContent): String =
        exportJson.encodeToString(Document.serializer(), Document(FORMAT_VERSION, content)) + "\n"

    fun decode(value: String): ResumeContent {
        val document = json.parseToJsonElement(value).jsonObject
        val version = document["formatVersion"]?.jsonPrimitive?.intOrNull
        require(version == FORMAT_VERSION) { "Unsupported resume content format version: $version" }
        return json.decodeFromJsonElement(Document.serializer(), document).content
    }

    /** Wraps persisted resume content with its independent format version. */
    @Serializable
    private data class Document(val formatVersion: Int, val content: ResumeContent)
}
