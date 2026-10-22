package reframecv.database.providers

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Persists runner definitions and execution history with their owning connections. */
@Dao
abstract class ConnectionRunnersDao {
    @Query("SELECT * FROM connection_runners WHERE connection_id = :connectionId ORDER BY id")
    abstract fun observe(connectionId: Long): Flow<List<ConnectionRunnerEntity>>

    @Query("SELECT * FROM connection_runners WHERE id = :id")
    abstract suspend fun get(id: Long): ConnectionRunnerEntity?

    @Insert
    abstract suspend fun insert(runner: ConnectionRunnerEntity): Long

    @Update
    abstract suspend fun update(runner: ConnectionRunnerEntity): Int

    @Transaction
    open suspend fun save(runner: ConnectionRunnerEntity): Long {
        if (runner.id == 0L) return insert(runner)
        val previous = checkNotNull(get(runner.id))
        require(previous.connectionId == runner.connectionId)
        check(update(runner.copy(createdAt = previous.createdAt)) == 1)
        return runner.id
    }

    @Query("DELETE FROM connection_runners WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("SELECT * FROM connection_runs WHERE runner_id = :runnerId ORDER BY id DESC")
    abstract fun runs(runnerId: Long): Flow<List<ConnectionRunEntity>>

    @Insert
    abstract suspend fun insertRun(run: ConnectionRunEntity): Long

    @Update
    abstract suspend fun writeRun(run: ConnectionRunEntity): Int

    @Query("SELECT * FROM connection_runs WHERE id = :id")
    abstract suspend fun getRun(id: Long): ConnectionRunEntity?

    @Transaction
    open suspend fun updateRun(run: ConnectionRunEntity): Int {
        val previous = checkNotNull(getRun(run.id))
        require(previous.runnerId == run.runnerId)
        require(previous.configurationSnapshot == run.configurationSnapshot)
        require(previous.startedAt == run.startedAt)
        return writeRun(
            run.copy(
                nextResultOffset = previous.nextResultOffset,
                resultTotal = previous.resultTotal,
                resultsComplete = previous.resultsComplete,
                resultColumns = previous.resultColumns,
            ),
        )
    }
}
