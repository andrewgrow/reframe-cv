package reframecv.database.coverletter

import androidx.room3.Insert
import androidx.room3.Query
import reframecv.database.vacancy.VacancyLetterEntity

/** Appends generated letters without replacing other vacancy fields or links. */
interface CoverLetterGenerationQueries {
    @Query("SELECT project_id FROM vacancies WHERE id = :id")
    suspend fun vacancyProject(id: Long): Long?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM vacancies_letters WHERE vacancy_id = :id")
    suspend fun nextLetterPosition(id: Long): Int

    @Insert
    suspend fun insertVacancyLetter(link: VacancyLetterEntity)

    @Query("UPDATE vacancies SET updated_at = :updatedAt WHERE id = :id")
    suspend fun touchVacancy(id: Long, updatedAt: Long): Int
}
