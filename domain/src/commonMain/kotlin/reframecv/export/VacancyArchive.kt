package reframecv.export

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy

/** Carries the chosen linked documents in their archive numbering order. */
data class VacancyArchive(
    val vacancy: Vacancy,
    val name: String,
    val resumes: List<Resume>,
    val coverLetters: List<CoverLetter>,
)

/** Exports an application document bundle through platform-specific archive handling. */
fun interface VacancyArchiveExporter {
    suspend fun export(archive: VacancyArchive): DocumentExportResult
}

/** Supplies only documents linked to the saved vacancy and owned by its project. */
data class VacancyArchiveChoices(val resumes: List<Resume>, val coverLetters: List<CoverLetter>)

fun archiveFilePrefix(name: String): String = name.trim()
    .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "-")
    .trim(' ', '.')
    .take(MAX_ARCHIVE_PREFIX_LENGTH)
    .trimEnd(' ', '.')

private const val MAX_ARCHIVE_PREFIX_LENGTH = 120
