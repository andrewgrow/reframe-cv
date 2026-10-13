package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.domain.models.resume.Resume

class TestResumesRepository : ResumesRepository {
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
        records.value = records.value.filterNot { it.id == id }
    }
}
