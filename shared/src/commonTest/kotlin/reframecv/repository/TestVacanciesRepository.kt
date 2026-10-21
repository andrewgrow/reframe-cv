package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.assessment.ResumeAssessment
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume

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
    override suspend fun saveResumeAssessment(
        vacancyId: Long,
        projectId: Long,
        resumeId: Long,
        assessment: ResumeAssessment,
    ) {
        val record = checkNotNull(
            records.value.find {
                it.id == vacancyId &&
                    it.projectId == projectId
            },
        )
        val link = VacancyResume(resumeId, assessment.result.score, assessment)
        val links = if (record.resumes.any { it.resumeId == resumeId }) {
            record.resumes.map { if (it.resumeId == resumeId) link else it }
        } else {
            record.resumes + link
        }
        update(record.copy(resumes = links))
    }
    override suspend fun deleteMany(projectId: Long, ids: Set<Long>) {
        require(ids.all { id -> records.value.any { it.id == id && it.projectId == projectId } })
        records.value = records.value.filterNot { it.id in ids }
    }
    override suspend fun delete(id: Long) {
        records.value = records.value.filterNot { it.id == id }
    }
}
