package reframecv.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import reframecv.domain.models.vacancy.Vacancy

/** Describes portable vacancy content independently of local IDs and source files. */
@Serializable
internal data class VacancyExportContent(
    val name: String,
    val jobTitle: String,
    val company: String,
    val description: String,
    val url: String,
    val keywords: List<String>,
    val responsibilities: List<String>,
    val requirements: List<String>,
    val niceToHave: List<String>,
)

/** Serializes a saved vacancy as formatted, versioned JSON for external processing. */
internal object VacancyExportJson {
    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(vacancy: Vacancy): String {
        val content = VacancyExportContent(
            vacancy.name,
            vacancy.jobTitle,
            vacancy.company,
            vacancy.description,
            vacancy.url,
            vacancy.keywords,
            vacancy.responsibilities,
            vacancy.requirements,
            vacancy.niceToHave,
        )
        return json.encodeToString(Document.serializer(), Document(1, content)) + "\n"
    }

    /** Wraps vacancy content with its independent export format version. */
    @Serializable
    private data class Document(val formatVersion: Int, val content: VacancyExportContent)
}
