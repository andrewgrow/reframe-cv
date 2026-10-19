package reframecv.providers.assessment

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import reframecv.assessment.ResumeAssessmentJson
import reframecv.assessment.ResumeAssessmentResult

class ResumeAssessmentContractTest {
    private val text = resumeAssessmentSchema()
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
        it.schemas(mapOf("urn:reframecv:resume-assessment:1" to text))
    }
    private val schema = registry.getSchema(SchemaLocation.of("urn:reframecv:resume-assessment:1"))

    @Test
    fun schemaAndRuntimeAcceptTheSameMinimalResultAndBoundaryScores() {
        val meta = registry.getSchema(
            SchemaLocation.of("https://json-schema.org/draft/2020-12/schema"),
        )
        assertTrue(meta.validate(text, InputFormat.JSON).isEmpty())
        for (score in listOf(0, 75, 100)) {
            val value =
                ResumeAssessmentResult(
                    score,
                    "Relevant Kotlin experience; leadership is not evidenced.",
                )
            val encoded = ResumeAssessmentJson.encodeResult(value)
            assertTrue(schema.validate(encoded, InputFormat.JSON).isEmpty())
            assertEquals(value, ResumeAssessmentJson.decodeResult(encoded))
        }
    }

    @Test
    fun invalidIncompleteAndUnexpectedResultsAreRejected() {
        val values = listOf(
            "{}", "[]", "not JSON",
            """{"score":101,"summary":"Too high"}""",
            """{"score":-1,"summary":"Negative"}""",
            """{"score":"80","summary":"String score"}""",
            """{"score":80.5,"summary":"Fraction"}""",
            """{"score":80,"summary":"  "}""",
            """{"score":80,"summary":123}""",
            """{"score":80,"summary":"Valid","extra":true}""",
        )
        values.forEach {
            assertFailsWith<IllegalArgumentException>(it) { ResumeAssessmentJson.decodeResult(it) }
            assertTrue(
                schema.validate(
                    it.takeIf {
                        it != "not JSON"
                    } ?: "{}",
                    InputFormat.JSON,
                ).isNotEmpty(),
            )
        }
    }
}
