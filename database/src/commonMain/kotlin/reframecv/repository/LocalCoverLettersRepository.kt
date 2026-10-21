package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.coverletter.CoverLetterDao
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.shared.nowMillis

/** Keeps letter drafts in local storage with permanent deletion. */
class LocalCoverLettersRepository(private val dao: CoverLetterDao) : CoverLettersRepository {
    override fun observeCoverLetters(projectId: Long) =
        dao.observeActive(projectId).map { records ->
            records.map { it.toDomainModel() }
        }

    override suspend fun search(projectId: Long, keyword: String) =
        dao.search(projectId, keyword).map { it.toDomainModel() }

    override suspend fun create(record: CoverLetter): Long {
        require(record.id == 0L) { "New records must not have an identifier" }
        val timestamp = nowMillis()
        return dao.create(record.copy(createdAt = timestamp, updatedAt = timestamp))
    }

    override suspend fun createAndLink(record: CoverLetter, vacancyId: Long?): CoverLetter {
        require(record.id == 0L)
        val time = nowMillis()
        return dao.createAndLink(record.copy(createdAt = time, updatedAt = time), vacancyId)
    }

    override suspend fun update(record: CoverLetter) {
        dao.update(record.copy(updatedAt = nowMillis()))
    }

    override suspend fun deleteMany(projectId: Long, ids: Set<Long>) {
        dao.deleteMany(projectId, ids, nowMillis())
    }

    override suspend fun delete(id: Long) {
        check(dao.delete(id, nowMillis()) == 1) { "Active record does not exist" }
    }
}
