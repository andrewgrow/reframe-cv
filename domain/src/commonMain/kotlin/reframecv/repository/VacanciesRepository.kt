package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.assessment.ResumeAssessment
import reframecv.attachments.SnapshotChange
import reframecv.attachments.SnapshotInfo
import reframecv.domain.models.vacancy.Vacancy

/** Lets features manage and find project vacancies independently of their storage. */
interface VacanciesRepository : VacancySnapshotsRepository {
    fun observeVacancies(projectId: Long): Flow<List<Vacancy>>
    suspend fun search(projectId: Long, keyword: String): List<Vacancy>
    suspend fun create(record: Vacancy): Long
    suspend fun createSaved(record: Vacancy, snapshot: SnapshotChange): Vacancy {
        val id = if (snapshot == SnapshotChange.Keep) create(record) else create(record, snapshot)
        return record.copy(id = id)
    }
    suspend fun update(record: Vacancy)
    suspend fun create(record: Vacancy, snapshot: SnapshotChange): Long {
        require(snapshot == SnapshotChange.Keep) { "Snapshot storage is not available" }
        return create(record)
    }
    suspend fun update(record: Vacancy, snapshot: SnapshotChange) {
        require(snapshot == SnapshotChange.Keep) { "Snapshot storage is not available" }
        update(record)
    }
    suspend fun saveResumeAssessment(
        vacancyId: Long,
        projectId: Long,
        resumeId: Long,
        assessment: ResumeAssessment,
    )
    suspend fun deleteMany(projectId: Long, ids: Set<Long>) {
        error("Bulk vacancy deletion is not available")
    }
    suspend fun delete(id: Long)
}

/** Provides access to a vacancy's saved source file independently of record editing. */
interface VacancySnapshotsRepository {
    suspend fun snapshot(vacancyId: Long): SnapshotInfo? = null
    suspend fun openSnapshot(vacancyId: Long) = Unit
}
