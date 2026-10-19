package reframecv.assessment

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

private const val MAX_MATCH_SCORE = 100

/** Represents an AI estimate of resume suitability for one vacancy, not an ATS prediction. */
@Serializable
data class ResumeAssessmentResult(val score: Int, val summary: String) {
    init {
        require(score in 0..MAX_MATCH_SCORE) { "Assessment score must be between 0 and 100" }
        require(summary.isNotBlank()) { "Assessment summary must not be blank" }
    }
}

/** Keeps a matching result together with its source versions and provider context. */
@Serializable
data class ResumeAssessment(
    val result: ResumeAssessmentResult,
    val assessedAt: Long,
    val resumeUpdatedAt: Long,
    val vacancyJson: String,
    val providerName: String,
    val modelName: String,
)

/** Encodes saved assessments and validates structured model responses. */
object ResumeAssessmentJson {
    private val json = Json { encodeDefaults = true }
    fun decodeResult(value: String): ResumeAssessmentResult {
        val document = json.parseToJsonElement(value) as? JsonObject
        val score = document?.get("score") as? JsonPrimitive
        val summary = document?.get("summary") as? JsonPrimitive
        require(score != null && !score.isString && score.intOrNull != null)
        require(summary?.isString == true)
        return json.decodeFromString(value)
    }
    fun encodeResult(value: ResumeAssessmentResult): String = json.encodeToString(value)
    fun encode(value: ResumeAssessment): String = json.encodeToString(value)
    fun decode(value: String): ResumeAssessment = json.decodeFromString(value)
}
