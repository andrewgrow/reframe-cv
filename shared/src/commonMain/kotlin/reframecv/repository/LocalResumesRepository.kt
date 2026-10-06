package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.resume.ResumeDao
import reframecv.domain.models.resume.Resume
import reframecv.shared.nowMillis

class LocalResumesRepository(private val dao: ResumeDao) : ResumesRepository {
    override fun observeResumes(projectId: Long) = dao.observeActive(projectId).map { records ->
        records.map { it.toDomainModel() }
    }

    override suspend fun search(projectId: Long, keyword: String) =
        dao.search(projectId, keyword).map { it.toDomainModel() }

    override suspend fun create(record: Resume): Long {
        require(record.id == 0L) { "New records must not have an identifier" }
        val timestamp = nowMillis()
        return dao.create(record.copy(createdAt = timestamp, updatedAt = timestamp))
    }

    override suspend fun update(record: Resume) {
        dao.update(record.copy(updatedAt = nowMillis()))
    }

    override suspend fun delete(id: Long) {
        check(dao.softDelete(id, nowMillis()) == 1) { "Active record does not exist" }
    }
}
