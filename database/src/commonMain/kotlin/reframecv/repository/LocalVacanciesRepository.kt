package reframecv.repository

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import reframecv.assessment.ResumeAssessment
import reframecv.attachments.SnapshotChange
import reframecv.database.vacancy.VacancyDao
import reframecv.domain.models.vacancy.Vacancy
import reframecv.shared.nowMillis

/** Keeps vacancies in local storage and removes discarded opportunities permanently. */
class LocalVacanciesRepository(
    private val dao: VacancyDao,
    private val snapshots: VacancySnapshotManager? = null,
) : VacanciesRepository,
    VacancySnapshotsRepository by LocalVacancySnapshots(snapshots) {
    override fun observeVacancies(projectId: Long) = flow {
        snapshots?.cleanup()
        emitAll(dao.observeActive(projectId).map { records -> records.map { it.toDomainModel() } })
    }

    override suspend fun search(projectId: Long, keyword: String) =
        dao.search(projectId, keyword).map { it.toDomainModel() }

    override suspend fun create(record: Vacancy): Long = create(record, SnapshotChange.Keep)

    override suspend fun create(record: Vacancy, snapshot: SnapshotChange): Long =
        createSaved(record, snapshot).id

    override suspend fun createSaved(record: Vacancy, snapshot: SnapshotChange): Vacancy {
        require(record.id == 0L) { "New records must not have an identifier" }
        val timestamp = nowMillis()
        val updated = record.copy(createdAt = timestamp, updatedAt = timestamp)
        return if (snapshots != null) {
            snapshots.createSaved(updated, snapshot)
        } else {
            require(snapshot == SnapshotChange.Keep)
            updated.copy(id = dao.create(updated))
        }
    }

    override suspend fun update(record: Vacancy) = update(record, SnapshotChange.Keep)

    override suspend fun update(record: Vacancy, snapshot: SnapshotChange) {
        val updated = record.copy(updatedAt = nowMillis())
        if (snapshots != null) {
            snapshots.update(updated, snapshot)
        } else {
            require(snapshot == SnapshotChange.Keep)
            dao.update(updated)
        }
    }

    override suspend fun saveResumeAssessment(
        vacancyId: Long,
        projectId: Long,
        resumeId: Long,
        assessment: ResumeAssessment,
    ) = dao.saveResumeAssessment(vacancyId, projectId, resumeId, assessment, nowMillis())

    override suspend fun deleteMany(projectId: Long, ids: Set<Long>) {
        if (snapshots != null) {
            snapshots.deleteMany(projectId, ids, nowMillis())
        } else {
            dao.deleteMany(projectId, ids, nowMillis())
        }
    }

    override suspend fun markApplied(projectId: Long, ids: Set<Long>, timestamp: Long) {
        dao.markApplied(projectId, ids.toList(), timestamp)
    }

    override suspend fun delete(id: Long) {
        if (snapshots != null) {
            snapshots.delete(id, nowMillis())
        } else {
            check(dao.delete(id, nowMillis()) == 1) { "Active record does not exist" }
        }
    }
}

/** Adapts source-file access to the optional platform snapshot manager. */
private class LocalVacancySnapshots(private val manager: VacancySnapshotManager?) :
    VacancySnapshotsRepository {
    override suspend fun snapshot(vacancyId: Long) = manager?.snapshot(vacancyId)
    override suspend fun openSnapshot(vacancyId: Long) {
        manager?.open(vacancyId)
    }
}
