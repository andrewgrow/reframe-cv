package reframecv.relevance

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import reframecv.domain.models.providers.ProviderConnection

private const val MAX_RELEVANCE_SCORE = 100

/** Represents how well one source vacancy matches the criteria in a user prompt. */
@Serializable
data class RunnerRelevanceScore(val score: Int) {
    init {
        require(score in 0..MAX_RELEVANCE_SCORE)
    }
}

/** Rejects malformed, extra or non-integer fields before a score can replace saved feedback. */
object RunnerRelevanceJson {
    private val json = Json
    fun decode(value: String): RunnerRelevanceScore {
        val document = json.parseToJsonElement(value) as? JsonObject
        val score = document?.get("score") as? JsonPrimitive
        require(score != null && !score.isString && score.intOrNull != null)
        return json.decodeFromString(value)
    }
}

/** Reports safe provider, contract or persistence failures to the AI request dialog. */
class RunnerRelevanceException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Scores a single original dataset row against user criteria without a resume or project. */
fun interface RunnerRelevanceRepository {
    suspend fun assess(
        sourceJson: String,
        connection: ProviderConnection,
        instruction: String,
    ): RunnerRelevanceScore
}
