package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.resume.Resume

/** Lets features manage and find project resumes independently of their storage. */
interface ResumesRepository : ResumeLinksRepository {
    fun observeResumes(projectId: Long): Flow<List<Resume>>
    fun observeMainResumeId(projectId: Long): Flow<Long?>
    suspend fun search(projectId: Long, keyword: String): List<Resume>
    suspend fun createSaved(record: Resume, mainResumeChange: MainResumeChange): Resume
    suspend fun createAdaptation(record: Resume, vacancyId: Long?): Resume
    suspend fun create(record: Resume): Long
    suspend fun create(record: Resume, mainResumeChange: MainResumeChange): Long
    suspend fun update(record: Resume)
    suspend fun update(record: Resume, mainResumeChange: MainResumeChange)
    suspend fun deleteMany(projectId: Long, ids: Set<Long>)
    suspend fun delete(id: Long)
}
