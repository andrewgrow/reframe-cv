package reframecv.usecase

import kotlinx.coroutines.flow.combine
import reframecv.domain.models.resume.Resume
import reframecv.repository.MainResumeChange
import reframecv.repository.ResumesRepository

/** Combines resume records with the project's main-resume selection. */
data class ResumeList(val resumes: List<Resume>, val mainResumeId: Long?)

/** Provides resume list and save/delete actions independently of storage. */
class ResumesUseCases(private val repository: ResumesRepository) {
    fun observe(projectId: Long) = combine(
        repository.observeResumes(projectId),
        repository.observeMainResumeId(projectId),
        ::ResumeList,
    )

    suspend fun save(record: Resume, mainResumeChange: MainResumeChange): Resume? {
        require(record.name.isNotBlank()) { "Resume name must not be blank" }
        if (record.id == 0L) return repository.createSaved(record, mainResumeChange)
        if (mainResumeChange == MainResumeChange.Keep) {
            repository.update(record)
        } else {
            repository.update(record, mainResumeChange)
        }
        return null
    }

    suspend fun delete(id: Long) = repository.delete(id)
}
