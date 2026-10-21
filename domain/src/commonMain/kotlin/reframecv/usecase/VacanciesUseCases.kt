package reframecv.usecase

import reframecv.attachments.SnapshotChange
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.VacanciesRepository

/** Provides vacancy list, persistence, and saved-snapshot actions to components. */
class VacanciesUseCases(private val repository: VacanciesRepository) {
    fun observe(projectId: Long) = repository.observeVacancies(projectId)

    suspend fun save(record: Vacancy, snapshot: SnapshotChange): Vacancy? {
        require(record.name.isNotBlank()) { "Vacancy name must not be blank" }
        if (record.id == 0L) return repository.createSaved(record, snapshot)
        if (snapshot == SnapshotChange.Keep) {
            repository.update(record)
        } else {
            repository.update(record, snapshot)
        }
        return null
    }

    suspend fun deleteMany(projectId: Long, ids: Set<Long>) = repository.deleteMany(projectId, ids)
    suspend fun delete(id: Long) = repository.delete(id)
    suspend fun loadSnapshot(id: Long) = repository.snapshot(id)
    suspend fun openSnapshot(id: Long) = repository.openSnapshot(id)
}
