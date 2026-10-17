package reframecv.recognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import reframecv.domain.models.vacancy.Vacancy

class VacancyRecognitionTest {
    @Test
    fun successfulResultMapsContentWithoutOverwritingIdentityLinksOrSourceMetadata() {
        val original = Vacancy(
            id = 7, projectId = 3, name = "Old", createdAt = 10, updatedAt = 20,
            company = "Company", url = "https://example.com/job", resumeId = 11,
            coverLetterId = 12, vacancyAttachmentId = 13,
        )
        val result = VacancyRecognitionJson.decode(recognitionFixture())
        assertEquals(
            original.copy(
                name = "Mobile developer",
                description = "Build mobile apps",
                keywords = listOf("Kotlin", "Android"),
                responsibilities = listOf("Build apps, APIs", "Work with teams\nReview code"),
                requirements = listOf("Kotlin"),
                niceToHave = listOf("CI/CD"),
            ),
            result.applyTo(original),
        )
        assertEquals(result, VacancyRecognitionJson.decode(VacancyRecognitionJson.encode(result)))
    }

    @Test
    fun supportsLegacyNiceToHaveTextNullAndListsWithoutGuessingTextBoundaries() {
        val cases = listOf(
            JsonPrimitive("Docker, CI/CD\nCloud") to listOf("Docker, CI/CD\nCloud"),
            JsonNull to emptyList(),
            JsonArray(listOf(JsonNull, JsonPrimitive(" SQL "), JsonPrimitive(""))) to listOf("SQL"),
        )
        cases.forEach { (value, expected) ->
            val result = VacancyRecognitionJson.decode(recognitionFixture("nice_to_have" to value))
            assertEquals(expected, result.applyTo(emptyVacancy()).niceToHave)
        }
    }

    @Test
    fun failuresAndUntitledSuccessCannotBecomeSavedVacancyContent() {
        listOf(400, 500).forEach { status ->
            val result = VacancyRecognitionJson.decode(
                recognitionFixture("statusCode" to JsonPrimitive(status), "jobTitle" to JsonNull),
            )
            assertFailsWith<IllegalArgumentException> { result.applyTo(emptyVacancy()) }
        }
        listOf(JsonNull, JsonPrimitive("  ")).forEach { title ->
            val result = VacancyRecognitionJson.decode(recognitionFixture("jobTitle" to title))
            assertFailsWith<IllegalArgumentException> { result.applyTo(emptyVacancy()) }
        }
    }

    @Test
    fun invalidResponsesAreRejectedRatherThanSilentlyChangingTheContract() {
        val values = listOf(
            "{}", "[]", "broken json",
            recognitionFixture("statusCode" to JsonPrimitive(201)),
            recognitionFixture("extra" to JsonPrimitive("ignored?")),
            recognitionFixture("requirements" to JsonPrimitive("Kotlin")),
            recognitionFixture("requirements" to JsonArray(listOf(JsonPrimitive(42)))),
            recognitionFixture("status" to JsonPrimitive(42)),
            recognitionFixture("jobTitle" to JsonPrimitive(false)),
            recognitionFixture("nice_to_have" to JsonPrimitive(42)),
        )
        values.forEach { value ->
            assertFailsWith<IllegalArgumentException>(value) {
                VacancyRecognitionJson.decode(value)
            }
        }
    }

    private fun emptyVacancy() = Vacancy(projectId = 3, name = "", createdAt = 0, updatedAt = 0)
}

internal fun recognitionFixture(
    vararg changes: Pair<String, kotlinx.serialization.json.JsonElement>,
): String {
    val original = Json.parseToJsonElement(
        """{
          "statusCode":200,"status":"OK","statusDetails":"Recognized",
          "jobTitle":" Mobile developer ","jobDescription":" Build mobile apps ",
          "keywords":[" Kotlin ",null,"kotlin","", "Android"],
          "responsibilities":[" Build apps, APIs ",null,"Work with teams\nReview code"],
          "requirements":[" Kotlin ","Kotlin", ""],"nice_to_have":[" CI/CD "]
        }""",
    ).jsonObject
    return JsonObject(original + changes).toString()
}
