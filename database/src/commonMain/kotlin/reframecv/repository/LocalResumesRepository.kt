package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.resume.ResumeDao
import reframecv.domain.models.resume.Resume
import reframecv.shared.nowMillis

/** Keeps resumes in local storage with permanent deletion. */
class LocalResumesRepository(
    private val dao: ResumeDao,
    private val photos: LocalResumePhotos? = null,
) : ResumesRepository,
    ResumeLinksRepository by LocalResumeLinksRepository(dao) {
    override fun observeResumes(projectId: Long) = dao.observeActive(projectId).map { records ->
        records.map { it.toDomainModel() }
    }

    override fun observeMainResumeId(projectId: Long) = dao.observeMainResumeId(projectId)

    override suspend fun search(projectId: Long, keyword: String) =
        dao.search(projectId, keyword).map { it.toDomainModel() }

    override suspend fun create(record: Resume): Long = create(record, MainResumeChange.Keep)

    override suspend fun create(record: Resume, mainResumeChange: MainResumeChange): Long =
        createSaved(record, mainResumeChange).id

    override suspend fun createSaved(record: Resume, mainResumeChange: MainResumeChange): Resume {
        require(record.id == 0L) { "New records must not have an identifier" }
        val timestamp = nowMillis()
        val persisted = record.copy(createdAt = timestamp, updatedAt = timestamp)
        val save = suspend { dao.create(persisted, mainResumeChange) }
        val id = photos?.save(record, save) ?: save()
        return persisted.copy(id = id)
    }

    override suspend fun createAdaptation(record: Resume, vacancyId: Long?): Resume {
        require(record.id == 0L && record.sourceResumeId != null)
        require(record.photoPath == null && record.keywords.isEmpty())
        val timestamp = nowMillis()
        val persisted = record.copy(createdAt = timestamp, updatedAt = timestamp)
        return persisted.copy(id = dao.createAdaptation(persisted, vacancyId))
    }

    override suspend fun update(record: Resume) = update(record, MainResumeChange.Keep)

    override suspend fun update(record: Resume, mainResumeChange: MainResumeChange) {
        val save = suspend { dao.update(record.copy(updatedAt = nowMillis()), mainResumeChange) }
        if (photos != null) photos.save(record, save) else save()
    }

    override suspend fun deleteMany(projectId: Long, ids: Set<Long>) {
        val remove = suspend { dao.deleteMany(projectId, ids, nowMillis()) }
        if (photos != null) photos.deleteMany(ids, remove) else remove()
    }

    override suspend fun delete(id: Long) {
        val remove = suspend {
            check(dao.delete(id, nowMillis()) == 1) { "Active record does not exist" }
        }
        if (photos != null) photos.delete(id, remove) else remove()
    }
}
