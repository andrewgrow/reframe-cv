package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.providers.ConnectionRunnersDao
import reframecv.database.providers.toEntity
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.shared.nowMillis

/** Keeps operation definitions and run history in the local database. */
class LocalConnectionRunnersRepository(private val dao: ConnectionRunnersDao) :
    ConnectionRunnersRepository {
    override fun observe(connectionId: Long) = dao.observe(connectionId).map { rows ->
        rows.map { it.toDomainModel() }
    }
    override suspend fun get(id: Long) = dao.get(id)?.toDomainModel()
    override suspend fun save(runner: ConnectionRunner): Long {
        val now = nowMillis()
        return dao.save(runner.copy(createdAt = now, updatedAt = now).toEntity())
    }
    override suspend fun delete(id: Long) = dao.delete(id)
    override fun runs(runnerId: Long) = dao.runs(runnerId).map { rows ->
        rows.map { it.toDomainModel() }
    }
    override suspend fun createRun(run: ConnectionRun): Long {
        require(run.id == 0L)
        return dao.insertRun(run.toEntity())
    }
    override suspend fun updateRun(run: ConnectionRun) {
        check(dao.updateRun(run.toEntity()) == 1)
    }
}
