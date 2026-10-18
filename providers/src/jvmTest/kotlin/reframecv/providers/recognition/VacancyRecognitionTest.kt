package reframecv.providers.recognition

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
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.applyTo

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
                jobTitle = "Mobile developer",
                company = "Recognized company",
                url = "https://example.com/recognized",
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
    fun unknownTitlesClearThePositionOnly() {
        listOf(JsonNull, JsonPrimitive("  ")).forEach { title ->
            val result = VacancyRecognitionJson.decode(recognitionFixture("jobTitle" to title))
            val updated = result.applyTo(
                emptyVacancy().copy(name = "My label", jobTitle = "Old title"),
            )
            assertEquals("", updated.jobTitle)
            assertEquals("My label", updated.name)
            assertEquals("", result.applyTo(emptyVacancy()).name)
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
            recognitionFixture("jobTitle" to JsonPrimitive(false)),
            recognitionFixture("company" to JsonPrimitive(42)),
            recognitionFixture("url" to JsonPrimitive(false)),
            recognitionFixture("nice_to_have" to JsonPrimitive(42)),
        )
        values.forEach { value ->
            assertFailsWith<IllegalArgumentException>(value) {
                VacancyRecognitionJson.decode(value)
            }
        }
    }

    @Test
    fun preservesUserNameEvenWhenEmpty() {
        val result = VacancyRecognitionJson.decode(recognitionFixture())
        assertEquals(
            "My shortlist",
            result.applyTo(emptyVacancy().copy(name = "My shortlist")).name,
        )
        assertEquals("", result.applyTo(emptyVacancy()).name)
        assertEquals("  ", result.applyTo(emptyVacancy().copy(name = "  ")).name)
    }

    @Test
    fun absentRecognizedContentClearsPreviousFieldsWithoutTouchingUserMetadata() {
        val result = VacancyRecognitionJson.decode(
            recognitionFixture(
                "company" to JsonNull,
                "url" to JsonNull,
                "jobDescription" to JsonNull,
                "keywords" to JsonArray(emptyList()),
                "responsibilities" to JsonArray(emptyList()),
                "requirements" to JsonArray(emptyList()),
                "nice_to_have" to JsonNull,
            ),
        )
        val original = emptyVacancy().copy(
            id = 5, name = "Keep my name", jobTitle = "Old position", company = "Old company",
            url = "https://old.example", description = "Old description", keywords = listOf("Old"),
            responsibilities = listOf(
                "Old",
            ),
            requirements = listOf("Old"), niceToHave = listOf("Old"),
            resumeId = 6, coverLetterId = 7, vacancyAttachmentId = 8,
        )
        assertEquals(
            original.copy(
                jobTitle = "Mobile developer",
                company = "",
                url = "",
                description = "",
                keywords = emptyList(),
                responsibilities = emptyList(),
                requirements = emptyList(),
                niceToHave = emptyList(),
            ),
            result.applyTo(original),
        )
    }

    private fun emptyVacancy() = Vacancy(projectId = 3, name = "", createdAt = 0, updatedAt = 0)
}

internal fun recognitionFixture(
    vararg changes: Pair<String, kotlinx.serialization.json.JsonElement>,
): String {
    val original = Json.parseToJsonElement(
        """{
          "jobTitle":" Mobile developer ","jobDescription":" Build mobile apps ",
          "company":" Recognized company ","url":" https://example.com/recognized ",
          "keywords":[" Kotlin ",null,"kotlin","", "Android"],
          "responsibilities":[" Build apps, APIs ",null,"Work with teams\nReview code"],
          "requirements":[" Kotlin ","Kotlin", ""],"nice_to_have":[" CI/CD "]
        }""",
    ).jsonObject
    return JsonObject(original + changes).toString()
}

private fun VacancyRecognitionResult.applyTo(vacancy: Vacancy): Vacancy {
    val outcome = toOutcome()
    require(outcome is RecognitionOutcome.Recognized) { "Cannot apply unsuccessful recognition" }
    return outcome.result.applyTo(vacancy)
}
