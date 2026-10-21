package reframecv.database.vacancy

import androidx.room3.Insert
import androidx.room3.Query

/** Replaces the saved resume links for a vacancy within its save transaction. */
interface VacancyResumeQueries {
    @Insert
    suspend fun insertLetters(records: List<VacancyLetterEntity>)

    @Query("DELETE FROM vacancies_letters WHERE vacancy_id = :id")
    suspend fun clearLetters(id: Long)

    @Insert
    suspend fun insertResumes(records: List<VacancyResumeEntity>)

    @Query("DELETE FROM vacancies_resumes WHERE vacancy_id = :id")
    suspend fun clearResumes(id: Long)
}
