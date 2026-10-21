package reframecv.database.vacancy

import androidx.room3.Transaction
import reframecv.database.workspace.WorkspaceDao
import reframecv.domain.models.vacancy.Vacancy

/** Validates and replaces the ordered letters during a vacancy save. */
interface VacancyLetterQueries :
    VacancyResumeQueries,
    WorkspaceDao {
    @Transaction
    suspend fun replaceLetters(id: Long, record: Vacancy) {
        require(record.coverLetterIds.distinct().size == record.coverLetterIds.size)
        record.coverLetterIds.forEach { requireActiveLetter(it, record.projectId) }
        clearLetters(id)
        insertLetters(
            record.coverLetterIds.mapIndexed { position, letter ->
                VacancyLetterEntity(id, letter, position)
            },
        )
    }
}
