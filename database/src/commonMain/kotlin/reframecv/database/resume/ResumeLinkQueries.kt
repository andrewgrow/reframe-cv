package reframecv.database.resume

import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import reframecv.database.DatabaseModel
import reframecv.domain.models.resume.ResumeVacancyLink

/** Represents the small database projection used by the resume list's Linked column. */
data class ResumeVacancyLinkRow(val resumeId: Long, val vacancyId: Long, val name: String) :
    DatabaseModel<ResumeVacancyLink> {
    override fun toDomainModel() = ResumeVacancyLink(resumeId, vacancyId, name)
}

/** Reads live vacancy links without materializing document content. */
interface ResumeLinkQueries {
    @Query(
        "SELECT vr.resume_id AS resumeId, v.id AS vacancyId, v.name AS name " +
            "FROM vacancies_resumes vr JOIN vacancies v ON v.id = vr.vacancy_id " +
            "JOIN resumes r ON r.id = vr.resume_id " +
            "WHERE v.project_id = :projectId AND r.project_id = :projectId ORDER BY v.id ASC",
    )
    fun observeLinkedVacancies(projectId: Long): Flow<List<ResumeVacancyLinkRow>>
}
