package reframecv.recognition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import reframecv.domain.models.vacancy.Vacancy

/** Describes a source recognition response independently of a saved vacancy. */
@Serializable
data class VacancyRecognitionResult(
    val statusCode: Int,
    val status: String,
    val statusDetails: String,
    val jobTitle: String?,
    val jobDescription: String?,
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

    fun applyTo(vacancy: Vacancy): Vacancy {
        require(statusCode == SUCCESS) { "Cannot apply unsuccessful vacancy recognition" }
        val title = jobTitle?.trim().orEmpty()
        require(title.isNotEmpty()) { "Recognized vacancy title must not be blank" }
        return vacancy.copy(
            name = title,
            description = jobDescription?.trim().orEmpty(),
            keywords = keywords.cleaned().distinctBy { it.lowercase() },
            responsibilities = responsibilities.cleaned(),
            requirements = requirements.cleaned(),
            niceToHave = niceToHave.cleaned(),
        )
    }

    companion object {
        const val SUCCESS = 200
        const val NOT_RECOGNIZED = 400
        const val INTERNAL_ERROR = 500
    }
}

private fun List<String?>.cleaned() = filterNotNull().map(String::trim)
    .filter(String::isNotEmpty).distinct()
