package reframecv.recognition

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class VacancyRecognitionSchemaTest {
    private val text = checkNotNull(
        javaClass.getResource("/vacancy-recognition-v1.schema.json"),
    ).readText()
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
        it.schemas(mapOf("urn:reframecv:vacancy-recognition:1" to text))
    }
    private val schema = registry.getSchema(
        SchemaLocation.of("urn:reframecv:vacancy-recognition:1"),
    )

    @Test
    fun schemaIsValidAndPropertiesAgreeWithSerializerIncludingSnakeCase() {
        val meta = registry.getSchema(
            SchemaLocation.of("https://json-schema.org/draft/2020-12/schema"),
        )
        assertTrue(meta.validate(text, InputFormat.JSON).isEmpty())
        val definition = Json.parseToJsonElement(text).jsonObject
        val serialized = Json.parseToJsonElement(
            VacancyRecognitionJson.encode(VacancyRecognitionJson.decode(recognitionFixture())),
        ).jsonObject
        assertEquals(definition.getValue("properties").jsonObject.keys, serialized.keys)
        assertTrue("nice_to_have" in serialized)
    }

    @Test
    fun fullLegacyAndFailureResultsPassSchemaAndDecode() {
        val values = listOf(
            recognitionFixture(),
            recognitionFixture("nice_to_have" to JsonPrimitive("Docker")),
            recognitionFixture("nice_to_have" to JsonNull),
            recognitionFixture("statusCode" to JsonPrimitive(400), "jobTitle" to JsonNull),
            recognitionFixture("statusCode" to JsonPrimitive(500), "jobDescription" to JsonNull),
        )
        values.forEach { value ->
            assertTrue(schema.validate(value, InputFormat.JSON).isEmpty(), value)
            val canonical = VacancyRecognitionJson.encode(VacancyRecognitionJson.decode(value))
            assertTrue(schema.validate(canonical, InputFormat.JSON).isEmpty(), canonical)
        }
    }

    @Test
    fun missingFieldsWrongTypesStatusAndUnknownPropertiesFailValidation() {
        val values = listOf(
            "{}",
            recognitionFixture("statusCode" to JsonPrimitive(201)),
            recognitionFixture("requirements" to JsonNull),
            recognitionFixture("keywords" to JsonArray(listOf(JsonPrimitive(42)))),
            recognitionFixture("nice_to_have" to JsonPrimitive(42)),
            recognitionFixture("jobTitle" to JsonPrimitive(false)),
            recognitionFixture("extra" to JsonPrimitive("unexpected")),
        )
        values.forEach { assertTrue(schema.validate(it, InputFormat.JSON).isNotEmpty(), it) }
    }
}
