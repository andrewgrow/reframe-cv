package reframecv.export

import reframecv.domain.models.vacancy.Vacancy
import reframecv.serialization.VacancyContentJson

/** Uses the same portable vacancy format for downloads and AI requests. */
internal object VacancyExportJson {
    fun encode(vacancy: Vacancy): String = VacancyContentJson.encode(vacancy)
}
