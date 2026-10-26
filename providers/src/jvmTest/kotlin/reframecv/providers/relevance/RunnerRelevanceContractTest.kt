package reframecv.providers.relevance

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import reframecv.relevance.RunnerRelevanceJson

class RunnerRelevanceContractTest {
    private val text = runnerRelevanceSchema()
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
        it.schemas(mapOf("urn:reframecv:runner-relevance:1" to text))
    }
    private val schema = registry.getSchema(SchemaLocation.of("urn:reframecv:runner-relevance:1"))

    @Test
    fun schemaAndDecoderAcceptBoundaryScoresAndRejectUnexpectedResponses() {
        for (score in listOf(0, 75, 100)) {
            val value = """{"score":$score}"""
            assertEquals(score, RunnerRelevanceJson.decode(value).score)
            assertTrue(schema.validate(value, InputFormat.JSON).isEmpty())
        }
        val invalid = listOf(
            "{}", "[]", "null", """{"score":-1}""", """{"score":101}""",
            """{"score":"80"}""", """{"score":true}""", """{"score":80.5}""",
            """{"score":80,"summary":"extra"}""", """{"score":null}""",
        )
        invalid.forEach { value ->
            assertFailsWith<IllegalArgumentException> { RunnerRelevanceJson.decode(value) }
            assertTrue(schema.validate(value, InputFormat.JSON).isNotEmpty())
        }
        assertFailsWith<IllegalArgumentException> { RunnerRelevanceJson.decode("not JSON") }
    }
}
