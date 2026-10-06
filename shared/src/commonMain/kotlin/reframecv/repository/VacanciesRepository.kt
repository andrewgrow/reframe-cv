package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.vacancy.Vacancy

interface VacanciesRepository {
    fun observeVacancies(projectId: Long): Flow<List<Vacancy>>
    suspend fun search(projectId: Long, keyword: String): List<Vacancy>
    suspend fun create(record: Vacancy): Long
    suspend fun update(record: Vacancy)
    suspend fun delete(id: Long)
}
