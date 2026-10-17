package reframecv.importing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import reframecv.domain.models.vacancy.Vacancy

/** Reads portable vacancy fields while ignoring unsupported fields and malformed values. */
internal object VacancyImportJson {
    fun decode(value: String): Vacancy {
        val document = Json.parseToJsonElement(value.removePrefix("\uFEFF")) as? JsonObject
        require(document != null) { "Vacancy document must be a JSON object" }
        val version = document["formatVersion"] as? JsonPrimitive
        require(version != null && !version.isString && version.intOrNull == 1) {
            "Unsupported vacancy document version"
        }
        val content = document["content"] as? JsonObject
        require(content != null) { "Vacancy document must contain a content object" }
        return Vacancy(
            projectId = 0, createdAt = 0, updatedAt = 0,
            name = if ("name" in content) content.text("name") else content.text("title"),
            jobTitle = content.text("jobTitle"), company = content.text("company"),
            description = content.text("description"), url = content.text("url"),
            keywords = content.strings("keywords"),
            responsibilities = content.strings("responsibilities"),
            requirements = content.strings("requirements"),
            niceToHave = content.strings("niceToHave"),
        )
    }

    private fun JsonObject.text(name: String): String =
        (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

    private fun JsonObject.strings(name: String): List<String> =
        (get(name) as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content
        }.orEmpty()
}
