package reframecv.logging

import io.klogging.noCoLogger
import java.io.IOException
import java.nio.file.Path
import kotlin.time.Instant
import reframecv.filesystem.applicationDataDirectory
import reframecv.shared.nowMillis

/** Appends error stacks beside the database and reports them through Klogging. */
class FileApplicationLogger(
    private val file: Path = applicationDataDirectory().resolve("stacktrace.txt"),
    private val history: EventHistory = EventHistory(),
) : ApplicationLogger {
    private val console = noCoLogger("ApplicationErrors")
    private val output = RotatingLogFile(file)

    override fun error(operation: String, error: Throwable, context: Map<String, Any?>) {
        val message = "$operation $context"
        runCatching { console.error(error, "{operation}", message) }
        val timestamp = Instant.fromEpochMilliseconds(nowMillis())
        val recentEvents = history.snapshot()
        val entry = buildString {
            appendLine("$timestamp ERROR $message")
            appendLine("Recent events (${recentEvents.size}):")
            recentEvents.forEach { appendLine(it) }
            appendLine("Stack trace:")
            appendLine(error.stackTraceToString())
            appendLine()
        }
        try {
            output.append(entry)
        } catch (failure: IOException) {
            reportWriteFailure(failure)
        } catch (failure: SecurityException) {
            reportWriteFailure(failure)
        }
    }

    private fun reportWriteFailure(error: Exception) {
        runCatching { console.error(error, "Could not append error log to {path}", file) }
        System.err.println("Could not append error log to $file: $error")
    }
}
