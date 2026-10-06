package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.resume.Resume

interface ResumesRepository {
    fun observeResumes(projectId: Long): Flow<List<Resume>>
    suspend fun search(projectId: Long, keyword: String): List<Resume>
    suspend fun create(record: Resume): Long
    suspend fun update(record: Resume)
    suspend fun delete(id: Long)
}
