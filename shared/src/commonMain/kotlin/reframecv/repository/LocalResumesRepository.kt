package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.resume.ResumeDao
import reframecv.domain.models.resume.Resume
import reframecv.shared.nowMillis

/** Keeps resumes in local storage with permanent deletion. */
class LocalResumesRepository(private val dao: ResumeDao) : ResumesRepository {
    override fun observeResumes(projectId: Long) = dao.observeActive(projectId).map { records ->
        records.map { it.toDomainModel() }
    }

    override fun observeMainResumeId(projectId: Long) = dao.observeMainResumeId(projectId)

    override suspend fun search(projectId: Long, keyword: String) =
        dao.search(projectId, keyword).map { it.toDomainModel() }

    override suspend fun create(record: Resume): Long = create(record, MainResumeChange.Keep)

    override suspend fun create(record: Resume, mainResumeChange: MainResumeChange): Long {
        require(record.id == 0L) { "New records must not have an identifier" }
        val timestamp = nowMillis()
        return dao.create(
            record.copy(createdAt = timestamp, updatedAt = timestamp),
            mainResumeChange,
        )
    }

    override suspend fun update(record: Resume) = update(record, MainResumeChange.Keep)

    override suspend fun update(record: Resume, mainResumeChange: MainResumeChange) {
        dao.update(record.copy(updatedAt = nowMillis()), mainResumeChange)
    }

    override suspend fun delete(id: Long) {
        check(dao.delete(id, nowMillis()) == 1) { "Active record does not exist" }
    }
}
