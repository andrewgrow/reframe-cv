package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.domain.models.coverletter.CoverLetter

class TestCoverLettersRepository : CoverLettersRepository {
    val records = MutableStateFlow<List<CoverLetter>>(emptyList())
    override fun observeCoverLetters(projectId: Long) = records.map { values ->
        values.filter { it.projectId == projectId && it.deletedAt == null }
    }
    override suspend fun search(projectId: Long, keyword: String) = records.value.filter { record ->
        record.projectId == projectId && record.deletedAt == null &&
            record.keywords.any { it.equals(keyword.trim(), ignoreCase = true) }
    }
    override suspend fun create(record: CoverLetter): Long {
        val id = (records.value.maxOfOrNull { it.id } ?: 0) + 1
        records.value += record.copy(id = id)
        return id
    }
    override suspend fun update(record: CoverLetter) {
        records.value = records.value.map { if (it.id == record.id) record else it }
    }
    override suspend fun delete(id: Long) {
        records.value = records.value.filterNot { it.id == id }
    }
}
