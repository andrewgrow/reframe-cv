package reframecv.database.resume

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun assertResumeSchemaMatches(descriptor: SerialDescriptor, definitions: JsonObject) {
    ResumeSchemaContract(definitions).compare(
        descriptor,
        definitions.getValue("ResumeContent").jsonObject,
    )
}

@OptIn(ExperimentalSerializationApi::class)
private class ResumeSchemaContract(private val definitions: JsonObject) {
    private val visited = mutableSetOf<String>()

    fun compare(type: SerialDescriptor, rule: JsonObject) {
        assertEquals(
            type.isNullable,
            rule["anyOf"] != null || rule["type"] is JsonArray,
            type.serialName,
        )
        val resolved = rule["anyOf"]?.jsonArray?.first()?.jsonObject ?: rule
        val schema = resolved["\$ref"]?.jsonPrimitive?.content?.let(::resolve) ?: resolved
        when (type.kind) {
            StructureKind.CLASS -> compareObject(type, schema)

            StructureKind.LIST -> {
                assertEquals(JsonPrimitive("array"), schema["type"], type.serialName)
                compare(type.getElementDescriptor(0), schema.getValue("items").jsonObject)
            }

            PolymorphicKind.SEALED -> compareSections(type, schema)

            else -> comparePrimitive(type, schema)
        }
    }

    private fun compareObject(type: SerialDescriptor, schema: JsonObject) {
        if (!visited.add(type.serialName)) return
        val properties = schema.getValue("properties").jsonObject
        val synthetic = if (properties["type"]?.jsonObject?.get("const") != null) {
            setOf("type")
        } else {
            emptySet()
        }
        val names = (0 until type.elementsCount).map(type::getElementName).toSet()
        assertEquals(names + synthetic, properties.keys, type.serialName)
        val required = (0 until type.elementsCount).filterNot(type::isElementOptional)
            .map(type::getElementName).toSet() + synthetic
        assertEquals(
            required,
            schema.getValue("required").jsonArray.map {
                it.jsonPrimitive.content
            }.toSet(),
            type.serialName,
        )
        assertEquals(JsonPrimitive(false), schema["additionalProperties"], type.serialName)
        for (index in 0 until type.elementsCount) {
            compare(
                type.getElementDescriptor(index),
                properties.getValue(type.getElementName(index)).jsonObject,
            )
        }
    }

    private fun compareSections(type: SerialDescriptor, schema: JsonObject) {
        val variants = type.getElementDescriptor(1)
        val alternatives = schema.getValue("oneOf").jsonArray.map {
            resolve(it.jsonObject.getValue("\$ref").jsonPrimitive.content)
        }
        assertEquals(
            (0 until variants.elementsCount).map {
                variants.getElementDescriptor(it).serialName
            }.toSet(),
            alternatives.map(::discriminator).toSet(),
        )
        for (index in 0 until variants.elementsCount) {
            val variant = variants.getElementDescriptor(index)
            compare(variant, alternatives.single { discriminator(it) == variant.serialName })
        }
    }

    private fun comparePrimitive(type: SerialDescriptor, schema: JsonObject) {
        val expected = when (type.kind) {
            PrimitiveKind.STRING -> "string"
            PrimitiveKind.BOOLEAN -> "boolean"
            PrimitiveKind.INT -> "integer"
            else -> error("Uncovered serializer kind: ${type.kind}")
        }
        val schemaType = schema.getValue("type")
        if (schemaType is JsonArray) {
            assertEquals(
                setOf(expected, "null"),
                schemaType.map {
                    it.jsonPrimitive.content
                }.toSet(),
            )
            assertTrue(type.isNullable)
        } else {
            assertEquals(JsonPrimitive(expected), schemaType, type.serialName)
        }
    }

    private fun resolve(reference: String): JsonObject =
        definitions.getValue(reference.substringAfterLast('/')).jsonObject

    private fun discriminator(schema: JsonObject): String =
        schema.getValue("properties").jsonObject.getValue("type").jsonObject
            .getValue("const").jsonPrimitive.content
}
