package reframecv.database.resume

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import reframecv.domain.models.resume.content.ResumeConsent
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.serialization.ResumeContentJson

class ResumeContentSchemaTest {
    private val schemaText = checkNotNull(javaClass.getResource("/resume-content-v1.schema.json"))
        .readText()
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
        it.schemas(mapOf("urn:reframecv:resume-content:1" to schemaText))
    }
    private val schema = registry.getSchema(SchemaLocation.of("urn:reframecv:resume-content:1"))

    @Test
    fun schemaItselfIsValidAndMatchesTheSerializerContract() {
        val meta = registry.getSchema(
            SchemaLocation.of("https://json-schema.org/draft/2020-12/schema"),
        )
        assertTrue(meta.validate(schemaText, InputFormat.JSON).isEmpty())
        assertResumeSchemaMatches(
            ResumeContent.serializer().descriptor,
            Json.parseToJsonElement(schemaText).jsonObject.getValue("\$defs").jsonObject,
        )
    }

    @Test
    fun fullEmptyAndPartialDocumentsPassSchemaAndDecode() {
        val values = listOf(
            ResumeContentJson.encode(sampleResumeContent()),
            ResumeContentJson.encode(ResumeContent()),
            ResumeContentJson.encode(
                sampleResumeContent().copy(
                    language = "EN",
                    consent = ResumeConsent(text = "Consent\nText", visible = false),
                ),
            ),
            """{"formatVersion":1,"content":{"profile":{"name":"Alex"}}}""",
            """{"formatVersion":1,"content":{"consent":{}}}""",
            """{"formatVersion":1,"content":{"consent":{"text":"Consent"}}}""",
            """{"formatVersion":1,"content":{"consent":{"visible":false}}}""",
            """{"formatVersion":1,"content":{"sections":[{"type":"summary"}]}}""",
        )
        values.forEach { value ->
            val errors = schema.validate(value, InputFormat.JSON)
            assertTrue(errors.isEmpty(), errors.toString())
            assertEquals(
                ResumeContentJson.decode(value),
                ResumeContentJson.decode(
                    ResumeContentJson.encode(ResumeContentJson.decode(value)),
                ),
            )
        }
    }

    @Test
    fun rejectsWrongVersionsUnknownFieldsTypesAndNulls() {
        val values = listOf(
            """{"formatVersion":2,"content":{}}""",
            """{"content":{}}""",
            """{"formatVersion":1}""",
            """{"formatVersion":1,"content":{"unknown":true}}""",
            """{"formatVersion":1,"content":{"profile":null}}""",
            """{"formatVersion":1,"content":{"language":null}}""",
            """{"formatVersion":1,"content":{"consent":null}}""",
            """{"formatVersion":1,"content":{"consent":{"unknown":true}}}""",
            """{"formatVersion":1,"content":{"consent":{"text":42}}}""",
            """{"formatVersion":1,"content":{"consent":{"visible":"yes"}}}""",
            """{"formatVersion":1,"content":{"sections":[{}]}}""",
            """{"formatVersion":1,"content":{"sections":[{"type":"future"}]}}""",
            """{"formatVersion":1,"content":{"sections":[{"type":"summary","text":null}]}}""",
            """{"formatVersion":1,"content":{"sections":[{"type":"summary","visible":"yes"}]}}""",
        )
        values.forEach { assertTrue(schema.validate(it, InputFormat.JSON).isNotEmpty(), it) }
    }

    @Test
    fun dateConstraintsAgreeWithModelsForPartialAndOngoingPeriods() {
        val schema = registry.getSchema(
            SchemaLocation.of(
                "urn:reframecv:resume-content:1#/\$defs/ResumeDateRange",
            ),
        )
        listOf("{}", """{"start":{"year":2020}}""", """{"isCurrent":true,"end":null}""")
            .forEach { assertTrue(schema.validate(it, InputFormat.JSON).isEmpty(), it) }
        listOf(
            """{"start":{"year":0}}""",
            """{"start":{"year":10000}}""",
            """{"start":{"year":2020,"month":13}}""",
            """{"start":{"year":2020,"month":0}}""",
            """{"start":{"month":1}}""",
            """{"isCurrent":true,"end":{"year":2020}}""",
        ).forEach { assertTrue(schema.validate(it, InputFormat.JSON).isNotEmpty(), it) }
    }
}
