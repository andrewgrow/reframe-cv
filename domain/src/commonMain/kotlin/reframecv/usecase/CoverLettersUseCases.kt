package reframecv.usecase

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.repository.CoverLettersRepository

/** Provides letter browsing, editing, and deletion independently of storage. */
class CoverLettersUseCases(private val repository: CoverLettersRepository) {
    fun observe(projectId: Long) = repository.observeCoverLetters(projectId)
    suspend fun save(record: CoverLetter): CoverLetter {
        require(record.id >= 0 && record.name.isNotBlank())
        val prepared = record.copy(name = record.name.trim())
        return if (prepared.id == 0L) {
            repository.createAndLink(prepared, null)
        } else {
            repository.update(prepared)
            prepared
        }
    }
    suspend fun deleteMany(projectId: Long, ids: Set<Long>) = repository.deleteMany(projectId, ids)
    suspend fun delete(id: Long) = repository.delete(id)
}
