package reframecv.usecase

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.repository.CoverLettersRepository

/** Provides letter browsing, editing, and deletion independently of storage. */
class CoverLettersUseCases(private val repository: CoverLettersRepository) {
    fun observe(projectId: Long) = repository.observeCoverLetters(projectId)
    suspend fun save(record: CoverLetter) {
        require(record.id > 0 && record.name.isNotBlank())
        repository.update(record.copy(name = record.name.trim()))
    }
    suspend fun delete(id: Long) = repository.delete(id)
}
