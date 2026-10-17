package reframecv.recognition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Describes a source recognition response independently of a saved vacancy. */
@Serializable
data class VacancyRecognitionResult(
    val statusCode: Int,
    val status: String,
    val statusDetails: String,
    val jobTitle: String?,
    val jobDescription: String?,
    val company: String?,
    val url: String?,
    val keywords: List<String?>,
    val responsibilities: List<String?>,
    val requirements: List<String?>,
    @SerialName("nice_to_have")
    val niceToHave: List<String?>,
) {
    init {
        require(statusCode in setOf(SUCCESS, NOT_RECOGNIZED, INTERNAL_ERROR)) {
            "Unsupported vacancy recognition status: $statusCode"
        }
    }

    companion object {
        const val SUCCESS = 200
        const val NOT_RECOGNIZED = 400
        const val INTERNAL_ERROR = 500
    }
}
