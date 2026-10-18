package reframecv.providers.recognition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Describes a source recognition response independently of a saved vacancy. */
@Serializable
data class VacancyRecognitionResult(
    val jobTitle: String?,
    val jobDescription: String?,
    val company: String?,
    val url: String?,
    val responsibilities: List<String?>,
    val requirements: List<String?>,
    @SerialName("nice_to_have")
    val niceToHave: List<String?>,
)
