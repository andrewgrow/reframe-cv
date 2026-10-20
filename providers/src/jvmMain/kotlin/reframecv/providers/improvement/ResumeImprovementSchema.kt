package reframecv.providers.improvement

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Adapts the portable resume schema to the provider's strict structured-output subset. */
fun resumeImprovementSchema(): String {
    val source = checkNotNull(
        RemoteResumeImprovementRepository::class.java.getResource("/resume-content-v1.schema.json"),
    ).readText()
    return strictSchema(Json.parseToJsonElement(source)).toString()
}

private fun strictSchema(value: JsonElement): JsonElement = when (value) {
    is JsonArray -> JsonArray(value.map(::strictSchema))

    is JsonObject -> {
        val fields = value.filterKeys {
            it !in
                setOf("\$schema", "\$id", "\$comment", "allOf", "default")
        }
            .mapKeys { (key, _) -> if (key == "oneOf") "anyOf" else key }
            .mapValues { (_, field) -> strictSchema(field) }.toMutableMap()
        (value["properties"] as? JsonObject)?.let { properties ->
            fields["required"] = JsonArray(properties.keys.map(::JsonPrimitive))
        }
        JsonObject(fields)
    }

    else -> value
}
