package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunner

class TestConnectionRunnersRepository : ConnectionRunnersRepository {
    val records = MutableStateFlow<List<ConnectionRunner>>(emptyList())
    val history = MutableStateFlow<List<ConnectionRun>>(emptyList())
    var failRead = false
    var beforeSave: suspend () -> Unit = {}
    var failSave = false
    var failDelete = false
    override fun observe(connectionId: Long) = records.map {
        check(!failRead)
        it.filter { runner -> runner.connectionId == connectionId }
    }
    override suspend fun get(id: Long) = records.value.find { it.id == id }
    override suspend fun save(runner: ConnectionRunner): Long {
        beforeSave()
        check(!failSave)
        val id = runner.id.takeIf { it != 0L } ?: ((records.value.maxOfOrNull { it.id } ?: 0) + 1)
        records.value = records.value.filterNot { it.id == id } + runner.copy(id = id)
        return id
    }
    override suspend fun delete(id: Long) {
        check(!failDelete)
        records.value = records.value.filterNot { it.id == id }
        history.value = history.value.filterNot { it.runnerId == id }
    }
    override fun runs(runnerId: Long) =
        history.map { it.filter { run -> run.runnerId == runnerId } }
    override suspend fun getRun(id: Long) = history.value.find { it.id == id }
    override suspend fun createRun(run: ConnectionRun): Long {
        val id = (history.value.maxOfOrNull { it.id } ?: 0) + 1
        history.value += run.copy(id = id)
        return id
    }
    override suspend fun updateRun(run: ConnectionRun) {
        history.value = history.value.map { if (it.id == run.id) run else it }
    }
}
