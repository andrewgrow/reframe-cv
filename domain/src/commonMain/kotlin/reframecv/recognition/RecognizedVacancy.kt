package reframecv.recognition

/** Describes recognized posting fields without provider response or transport details. */
data class RecognizedVacancy(
    val jobTitle: String,
    val description: String,
    val company: String,
    val url: String,
    val responsibilities: List<String>,
    val requirements: List<String>,
    val niceToHave: List<String>,
)
