package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.VacancyResume

class TestResumesRepository(private val vacancies: TestVacanciesRepository? = null) :
    ResumesRepository {
    val mainResumeIds = MutableStateFlow<Map<Long, Long?>>(emptyMap())
    override fun observeMainResumeId(projectId: Long) = mainResumeIds.map { it[projectId] }
    val records = MutableStateFlow<List<Resume>>(emptyList())
    override fun observeResumes(projectId: Long) = records.map { values ->
        values.filter { it.projectId == projectId }
    }
    override suspend fun search(projectId: Long, keyword: String) = records.value.filter { record ->
        record.projectId == projectId &&
            record.keywords.any { it.equals(keyword.trim(), ignoreCase = true) }
    }
    override suspend fun create(record: Resume): Long {
        val id = (records.value.maxOfOrNull { it.id } ?: 0) + 1
        records.value += record.copy(id = id)
        return id
    }
    override suspend fun create(record: Resume, mainResumeChange: MainResumeChange): Long {
        val id = create(record)
        changeMain(record.projectId, id, mainResumeChange)
        return id
    }
    override suspend fun createSaved(record: Resume, mainResumeChange: MainResumeChange): Resume {
        val id = create(record, mainResumeChange)
        return records.value.single { it.id == id }
    }
    override suspend fun createAdaptation(record: Resume, vacancyId: Long?): Resume {
        require(
            records.value.any {
                it.id == record.sourceResumeId &&
                    it.projectId == record.projectId
            },
        )
        val vacancy = vacancyId?.let { id ->
            checkNotNull(
                vacancies?.records?.value?.find {
                    it.id ==
                        id &&
                        it.projectId == record.projectId
                },
            )
        }
        val result = createSaved(record, MainResumeChange.Keep)
        if (vacancy != null) {
            vacancies!!.records.value = vacancies.records.value.map {
                if (it.id ==
                    vacancy.id
                ) {
                    it.copy(resumes = it.resumes + VacancyResume(result.id))
                } else {
                    it
                }
            }
        }
        return result
    }
    override suspend fun update(record: Resume, mainResumeChange: MainResumeChange) {
        update(record)
        changeMain(record.projectId, record.id, mainResumeChange)
    }
    private fun changeMain(projectId: Long, id: Long, change: MainResumeChange) {
        when (change) {
            MainResumeChange.Keep -> Unit

            MainResumeChange.Select -> mainResumeIds.value += projectId to id

            MainResumeChange.Clear -> if (mainResumeIds.value[projectId] == id) {
                mainResumeIds.value += projectId to null
            }
        }
    }
    override suspend fun update(record: Resume) {
        records.value = records.value.map { if (it.id == record.id) record else it }
    }
    override suspend fun delete(id: Long) {
        records.value = records.value.filterNot { it.id == id }.map {
            if (it.sourceResumeId == id) it.copy(sourceResumeId = null) else it
        }
        mainResumeIds.value =
            mainResumeIds.value.mapValues { (_, value) -> if (value == id) null else value }
    }
}
