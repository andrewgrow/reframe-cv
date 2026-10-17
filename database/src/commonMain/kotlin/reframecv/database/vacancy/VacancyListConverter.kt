package reframecv.database.vacancy

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/** Stores structured vacancy lists as JSON without treating prose as comma-separated keywords. */
class VacancyListConverter {
    @ColumnTypeConverter
    fun encode(values: List<String>): String = Json.encodeToString(values)

    @ColumnTypeConverter
    fun decode(value: String): List<String> {
        val values = Json.parseToJsonElement(value)
        require(values is JsonArray) { "Vacancy list must be a JSON array" }
        return values.map {
            require(it is JsonPrimitive && it.isString) { "Vacancy list items must be strings" }
            it.content
        }
    }
}
