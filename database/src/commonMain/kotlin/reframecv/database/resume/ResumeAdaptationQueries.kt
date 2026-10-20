package reframecv.database.resume

import androidx.room3.Insert
import androidx.room3.Query
import reframecv.database.vacancy.VacancyResumeEntity

/** Appends AI adaptation links without replacing other vacancy fields or resume links. */
interface ResumeAdaptationQueries {
    @Query("SELECT project_id FROM vacancies WHERE id = :id")
    suspend fun vacancyProject(id: Long): Long?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM vacancies_resumes WHERE vacancy_id = :id")
    suspend fun nextVacancyResumePosition(id: Long): Int

    @Insert
    suspend fun insertVacancyResume(link: VacancyResumeEntity)

    @Query("UPDATE vacancies SET updated_at = :updatedAt WHERE id = :id")
    suspend fun touchVacancy(id: Long, updatedAt: Long): Int
}
