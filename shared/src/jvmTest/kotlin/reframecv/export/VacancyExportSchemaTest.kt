package reframecv.export

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume

class VacancyExportSchemaTest {
    private val text = checkNotNull(
        javaClass.getResource("/vacancy-content-v1.schema.json"),
    ).readText()
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
        it.schemas(mapOf("urn:reframecv:vacancy-content:1" to text))
    }
    private val schema = registry.getSchema(SchemaLocation.of("urn:reframecv:vacancy-content:1"))

    @Test
    fun exportedEmptyAndFullContentMatchTheSchemaWithoutLocalOrProcessingMetadata() {
        val vacancy =
            Vacancy(
                id = 7,
                projectId = 1,
                name = "Résumé developer",
                createdAt = 10,
                updatedAt = 20,
            )
        listOf(
            vacancy,
            vacancy.copy(
                company = "Company", description = "One\nTwo", url = "https://example.com/role",
                keywords = listOf("Kotlin"), responsibilities = listOf("A, B"),
                requirements = listOf("C"), niceToHave = listOf("D"),
                resumes = listOf(
                    VacancyResume(2, 83),
                ),
                coverLetterIds = listOf(3), vacancyAttachmentId = 4,
            ),
        ).forEach { record ->
            val exported = VacancyExportJson.encode(record)
            assertTrue(schema.validate(exported, InputFormat.JSON).isEmpty(), exported)
            val content = Json.parseToJsonElement(
                exported,
            ).jsonObject.getValue("content").jsonObject
            val expectedKeys = Json.parseToJsonElement(
                text,
            ).jsonObject.getValue("\$defs").jsonObject
                .getValue("VacancyContent").jsonObject.getValue("properties").jsonObject.keys
            assertEquals(expectedKeys, content.keys)
            assertTrue(
                listOf(
                    "id",
                    "projectId",
                    "filePath",
                    "statusCode",
                    "vacancyAttachmentId",
                    "resumes",
                    "atsScore",
                ).none {
                    it in
                        content
                },
            )
        }
        val meta = registry.getSchema(
            SchemaLocation.of("https://json-schema.org/draft/2020-12/schema"),
        )
        assertTrue(meta.validate(text, InputFormat.JSON).isEmpty())
    }

    @Test
    fun wrongVersionUnknownFieldsWrongListItemsAndNullsAreRejected() {
        listOf(
            "{}",
            """{"formatVersion":2,"content":{}}""",
            """{"formatVersion":1,"content":null}""",
            VacancyExportJson.encode(
                Vacancy(projectId = 1, name = "A", createdAt = 0, updatedAt = 0),
            )
                .replace("\"requirements\": []", "\"requirements\": [42]"),
        ).forEach { assertTrue(schema.validate(it, InputFormat.JSON).isNotEmpty(), it) }
    }
}
