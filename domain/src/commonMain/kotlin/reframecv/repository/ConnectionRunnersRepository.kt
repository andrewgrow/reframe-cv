package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunner

/** Stores reusable operations and their execution history independently of provider protocols. */
interface ConnectionRunnersRepository {
    fun observe(connectionId: Long): Flow<List<ConnectionRunner>>
    suspend fun get(id: Long): ConnectionRunner?
    suspend fun save(runner: ConnectionRunner): Long
    suspend fun delete(id: Long)
    fun runs(runnerId: Long): Flow<List<ConnectionRun>>
    suspend fun createRun(run: ConnectionRun): Long
    suspend fun updateRun(run: ConnectionRun)
}
