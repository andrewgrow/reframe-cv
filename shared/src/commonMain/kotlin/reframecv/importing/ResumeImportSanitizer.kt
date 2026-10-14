package reframecv.importing

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/** Filters unsupported fields, entries and values using the resume serializer contract. */
@OptIn(ExperimentalSerializationApi::class)
internal object ResumeImportSanitizer {
    private const val MAX_YEAR = 9999
    private const val MAX_MONTH = 12

    fun sanitize(value: JsonElement, descriptor: SerialDescriptor): JsonElement? = when {
        value == JsonNull -> if (descriptor.isNullable) JsonNull else null

        descriptor.kind == PrimitiveKind.STRING ->
            (value as? JsonPrimitive)?.takeIf { it.isString }

        descriptor.kind == PrimitiveKind.BOOLEAN ->
            (value as? JsonPrimitive)?.takeIf { !it.isString && it.booleanOrNull != null }

        descriptor.kind == PrimitiveKind.INT ->
            (value as? JsonPrimitive)?.takeIf { !it.isString && it.intOrNull != null }

        descriptor.kind == StructureKind.LIST -> (value as? JsonArray)?.let { array ->
            JsonArray(array.mapNotNull { sanitize(it, descriptor.getElementDescriptor(0)) })
        }

        descriptor.kind == PolymorphicKind.SEALED -> section(value, descriptor)

        descriptor.kind == StructureKind.CLASS -> fields(value, descriptor)

        else -> null
    }

    private fun section(value: JsonElement, descriptor: SerialDescriptor): JsonElement? {
        val objectValue = value as? JsonObject
        val type = (objectValue?.get("type") as? JsonPrimitive)?.takeIf { it.isString }
        val variants = descriptor.getElementDescriptor(1)
        val variant = (0 until variants.elementsCount).map { variants.getElementDescriptor(it) }
            .find { it.serialName == type?.content }
        return if (objectValue != null && type != null && variant != null) {
            (fields(objectValue, variant) as? JsonObject)?.let { JsonObject(it + ("type" to type)) }
        } else {
            null
        }
    }

    private fun fields(value: JsonElement, descriptor: SerialDescriptor): JsonElement? {
        val objectValue = value as? JsonObject ?: return null
        val fields = buildMap {
            for (index in 0 until descriptor.elementsCount) {
                val name = descriptor.getElementName(index)
                objectValue[name]?.let { original ->
                    sanitize(original, descriptor.getElementDescriptor(index))?.let {
                        put(name, it)
                    }
                }
            }
        }.toMutableMap()
        if (descriptor.serialName.removeSuffix("?").endsWith(".ResumeDate")) {
            val year = (fields["year"] as? JsonPrimitive)?.intOrNull
            if (year == null || year !in 1..MAX_YEAR) fields.remove("year")
            val month = (fields["month"] as? JsonPrimitive)?.intOrNull
            if (month != null && month !in 1..MAX_MONTH) fields.remove("month")
        }
        if (descriptor.serialName.endsWith(".ResumeDateRange") &&
            (fields["isCurrent"] as? JsonPrimitive)?.booleanOrNull == true
        ) {
            fields.remove("end")
        }
        return if ((0 until descriptor.elementsCount).any {
                !descriptor.isElementOptional(it) && descriptor.getElementName(it) !in fields
            }
        ) {
            null
        } else {
            JsonObject(fields)
        }
    }
}
