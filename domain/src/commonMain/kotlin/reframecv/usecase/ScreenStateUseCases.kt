package reframecv.usecase

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import reframecv.domain.models.screenstate.ScreenState
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.logging.ApplicationLogger
import reframecv.repository.ScreenStatesRepository

/** Serializes view preference operations beyond page lifetimes and flushes them before shutdown. */
class ScreenStateUseCases(
    private val repository: ScreenStatesRepository,
    private val logger: ApplicationLogger,
) {
    /** Keeps reads behind pending writes so reopening a page restores its latest state. */
    private sealed interface Operation {
        data class Read(
            val owner: ScreenStateOwner,
            val result: CompletableDeferred<ScreenState?>,
        ) : Operation
        data class Write(val owner: ScreenStateOwner, val changes: JsonObject) : Operation
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val operations = Channel<Operation>(Channel.UNLIMITED)
    private val worker = scope.launch {
        for (operation in operations) {
            when (operation) {
                is Operation.Read -> read(operation)
                is Operation.Write -> write(operation.owner, operation.changes)
            }
        }
    }

    suspend fun load(owner: ScreenStateOwner): ScreenState? {
        val result = CompletableDeferred<ScreenState?>()
        operations.send(Operation.Read(owner, result))
        return result.await()
    }

    fun update(owner: ScreenStateOwner, changes: JsonObject) {
        require(changes.keys.all { it.isNotBlank() }) { "Screen state keys must not be blank" }
        check(operations.trySend(Operation.Write(owner, changes)).isSuccess) {
            "View preferences are closed"
        }
    }

    suspend fun close() {
        operations.close()
        worker.join()
        scope.cancel()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun read(operation: Operation.Read) {
        try {
            operation.result.complete(repository.get(operation.owner))
        } catch (error: CancellationException) {
            operation.result.completeExceptionally(error)
            throw error
        } catch (error: Exception) {
            operation.result.completeExceptionally(error)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun write(owner: ScreenStateOwner, changes: JsonObject) {
        try {
            repository.update(owner, changes)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.error(
                "Save screen state",
                error,
                mapOf("screen" to owner.screen, "documentId" to owner.documentId),
            )
        }
    }
}
