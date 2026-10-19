package reframecv.database.screenstate

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Keeps corrupt stored JSON from preventing a screen from opening. */
internal object ScreenStateJson {
    fun decode(value: String): JsonObject = try {
        Json.parseToJsonElement(value) as? JsonObject ?: JsonObject(emptyMap())
    } catch (_: SerializationException) {
        JsonObject(emptyMap())
    }
}
