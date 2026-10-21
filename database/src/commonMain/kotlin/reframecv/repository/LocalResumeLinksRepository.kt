package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.resume.ResumeDao

/** Supplies live vacancy names and identities for the resume list. */
class LocalResumeLinksRepository(private val dao: ResumeDao) : ResumeLinksRepository {
    override fun observeLinkedVacancies(projectId: Long) =
        dao.observeLinkedVacancies(projectId).map { rows -> rows.map { it.toDomainModel() } }
}
