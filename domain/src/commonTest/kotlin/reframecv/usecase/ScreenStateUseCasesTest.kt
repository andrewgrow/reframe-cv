package reframecv.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.screenstate.ScreenState
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.logging.ApplicationLogger
import reframecv.repository.ScreenStatesRepository

class ScreenStateUseCasesTest {
    private val owner = ScreenStateOwner(ScreenType.Settings)
    private fun change(expanded: Boolean) = buildJsonObject {
        put("job_details", if (expanded) "expanded" else "collapsed")
    }

    @Test
    fun serializesRapidTogglesAndFlushesPendingWritesOnClose() = runBlocking<Unit> {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val saved = mutableListOf<JsonObject>()
        val repository = object : ScreenStatesRepository {
            override suspend fun get(owner: ScreenStateOwner) =
                saved.lastOrNull()?.let { ScreenState(owner, it) }
            override suspend fun update(owner: ScreenStateOwner, changes: JsonObject) {
                entered.complete(Unit)
                release.await()
                saved += changes
            }
        }
        val useCases = ScreenStateUseCases(repository, ApplicationLogger.None)
        useCases.update(owner, change(false))
        withTimeout(5000.milliseconds) { entered.await() }
        useCases.update(owner, change(true))
        val closing = async(start = CoroutineStart.UNDISPATCHED) { useCases.close() }
        assertFalse(closing.isCompleted)
        release.complete(Unit)
        withTimeout(5000.milliseconds) { closing.await() }
        assertEquals(listOf(change(false), change(true)), saved)
        assertFailsWith<IllegalStateException> { useCases.update(owner, change(false)) }
    }

    @Test
    fun loadsAfterPendingWritesAndRecoversFromRepositoryFailures() = runBlocking<Unit> {
        val logged = Channel<Throwable>(Channel.UNLIMITED)
        var failWrite = true
        var failRead = false
        var value: ScreenState? = null
        val repository = object : ScreenStatesRepository {
            override suspend fun get(owner: ScreenStateOwner): ScreenState? {
                if (failRead) error("Read failed")
                return value
            }
            override suspend fun update(owner: ScreenStateOwner, changes: JsonObject) {
                if (failWrite) error("Write failed")
                value = ScreenState(owner, changes)
            }
        }
        val useCases =
            ScreenStateUseCases(
                repository,
                ApplicationLogger { _, error, _ ->
                    logged.trySend(error)
                },
            )
        try {
            useCases.update(owner, change(false))
            assertEquals(null, withTimeout(5000.milliseconds) { useCases.load(owner) })
            assertEquals("Write failed", logged.receive().message)
            failWrite = false
            useCases.update(owner, change(true))
            assertEquals("expanded", useCases.load(owner)?.text("job_details"))
            failRead = true
            assertFailsWith<IllegalStateException> { useCases.load(owner) }
            failRead = false
            assertEquals("expanded", useCases.load(owner)?.text("job_details"))
        } finally {
            useCases.close()
        }
    }
}
