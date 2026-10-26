package reframecv.documents.vacancy

/** Supplies shareable vacancy content independently of local application tracking and links. */
data class VacancyPrintData(
    val title: String,
    val company: String = "",
    val url: String = "",
    val description: String = "",
    val responsibilities: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val niceToHave: List<String> = emptyList(),
)
