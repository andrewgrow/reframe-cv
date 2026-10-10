package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.domain.models.vacancy.Vacancy

class TestVacanciesRepository : VacanciesRepository {
    val records = MutableStateFlow<List<Vacancy>>(emptyList())
    override fun observeVacancies(projectId: Long) = records.map { values ->
        values.filter { it.projectId == projectId }
    }
    override suspend fun search(projectId: Long, keyword: String) = records.value.filter { record ->
        record.projectId == projectId &&
            record.keywords.any { it.equals(keyword.trim(), ignoreCase = true) }
    }
    override suspend fun create(record: Vacancy): Long {
        val id = (records.value.maxOfOrNull { it.id } ?: 0) + 1
        records.value += record.copy(id = id)
        return id
    }
    override suspend fun update(record: Vacancy) {
        records.value = records.value.map { if (it.id == record.id) record else it }
    }
    override suspend fun delete(id: Long) {
        records.value = records.value.filterNot { it.id == id }
    }
}
