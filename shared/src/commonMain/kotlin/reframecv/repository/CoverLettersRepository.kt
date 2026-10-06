package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.coverletter.CoverLetter

interface CoverLettersRepository {
    fun observeCoverLetters(projectId: Long): Flow<List<CoverLetter>>
    suspend fun search(projectId: Long, keyword: String): List<CoverLetter>
    suspend fun create(record: CoverLetter): Long
    suspend fun update(record: CoverLetter)
    suspend fun delete(id: Long)
}
