package reframecv.importing

import reframecv.domain.models.vacancy.Vacancy

/** Holds portable vacancy fields and owns the temporary JSON source. */
interface ImportedVacancyFile {
    val vacancy: Vacancy
    suspend fun deleteTemporaryFile()
}

/** Lets vacancy creation select and read JSON through platform file handling. */
fun interface VacancyImporter {
    suspend fun pickAndImport(): ImportedVacancyFile?
}
