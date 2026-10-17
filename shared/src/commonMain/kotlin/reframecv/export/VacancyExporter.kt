package reframecv.export

import reframecv.domain.models.vacancy.Vacancy

/** Lets vacancy features export portable documents through platform-specific file handling. */
fun interface VacancyExporter {
    suspend fun export(vacancy: Vacancy): DocumentExportResult
}
