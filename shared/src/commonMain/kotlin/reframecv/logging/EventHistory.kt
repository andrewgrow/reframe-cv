package reframecv.logging

import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import reframecv.shared.nowMillis

/** Keeps the latest application events available for error reports. */
class EventHistory {
    private val events = MutableStateFlow<List<String>>(emptyList())

    fun record(summary: String) {
        val entry = "${Instant.fromEpochMilliseconds(nowMillis())} $summary"
        events.update { (it + entry).takeLast(CAPACITY) }
    }

    fun snapshot(): List<String> = events.value

    companion object {
        private const val CAPACITY = 200
    }
}
