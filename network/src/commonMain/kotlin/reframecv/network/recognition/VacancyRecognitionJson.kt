package reframecv.network.recognition

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** Reads the recognition contract, including divo-resume's legacy nice-to-have text. */
object VacancyRecognitionJson {
    private val json = Json { encodeDefaults = true }

    fun decode(value: String): VacancyRecognitionResult {
        val document = json.parseToJsonElement(value).jsonObject
        val niceToHave = document["nice_to_have"]
        val normalized = when {
            niceToHave == JsonNull -> document + ("nice_to_have" to JsonArray(emptyList()))

            niceToHave is JsonPrimitive && niceToHave.isString ->
                document + ("nice_to_have" to JsonArray(listOf(niceToHave)))

            else -> document
        }
        return json.decodeFromJsonElement(
            VacancyRecognitionResult.serializer(),
            JsonObject(normalized),
        )
    }

    fun encode(result: VacancyRecognitionResult): String =
        json.encodeToString(VacancyRecognitionResult.serializer(), result)
}
