package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.resume.ResumeVacancyLink

/** Observes vacancy references shown alongside project resumes. */
fun interface ResumeLinksRepository {
    fun observeLinkedVacancies(projectId: Long): Flow<List<ResumeVacancyLink>>
}
