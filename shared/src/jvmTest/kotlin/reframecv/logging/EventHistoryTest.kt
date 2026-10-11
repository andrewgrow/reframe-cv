package reframecv.logging

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventHistoryTest {
    @Test
    fun keepsOnlyTheLast200EventsInOrderAndSnapshotsRemainStable() {
        val history = EventHistory()
        assertEquals(emptyList(), history.snapshot())
        (1..210).forEach { history.record("Event $it") }
        val snapshot = history.snapshot()
        assertEquals((11..210).map { "Event $it" }, snapshot.map { it.substringAfter(' ') })
        assertTrue(snapshot.all { Regex("\\d{4}-\\d{2}-\\d{2}T").containsMatchIn(it) })
        history.record("Event 211")
        assertTrue(snapshot.last().endsWith("Event 210"))
        assertTrue(history.snapshot().last().endsWith("Event 211"))
    }

    @Test
    fun concurrentUpdatesDoNotLoseEvents() {
        val history = EventHistory()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..200).map { id -> executor.submit { history.record("Event $id") } }
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
        assertEquals(
            (1..200).map { "Event $it" }.toSet(),
            history.snapshot().map { it.substringAfter(' ') }.toSet(),
        )
    }
}
