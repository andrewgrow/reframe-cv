package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.resume.ResumeDao
import reframecv.domain.models.resume.Resume
import reframecv.shared.nowMillis

/** Keeps resumes in local storage with permanent deletion. */
class LocalResumesRepository(
    private val dao: ResumeDao,
    private val photos: LocalResumePhotos? = null,
) : ResumesRepository {
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
        val save = suspend {
            dao.create(record.copy(createdAt = timestamp, updatedAt = timestamp), mainResumeChange)
        }
        return photos?.save(record, save) ?: save()
    }

    override suspend fun update(record: Resume) = update(record, MainResumeChange.Keep)

    override suspend fun update(record: Resume, mainResumeChange: MainResumeChange) {
        val save = suspend { dao.update(record.copy(updatedAt = nowMillis()), mainResumeChange) }
        if (photos != null) photos.save(record, save) else save()
    }

    override suspend fun delete(id: Long) {
        val remove = suspend {
            check(dao.delete(id, nowMillis()) == 1) { "Active record does not exist" }
        }
        if (photos != null) photos.delete(id, remove) else remove()
    }
}
