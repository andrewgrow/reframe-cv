package reframecv.network

import io.klogging.noCoLogger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Writes sanitized HTTP diagnostics to the console and a file without blocking the UI thread. */
class FileHttpTrafficLogger(private val file: Path) : HttpTrafficLogger {
    private val console = noCoLogger("OpenRouterHttp")

    override suspend fun write(message: String) = withContext(Dispatchers.IO) {
        runCatching { console.info("{exchange}", message) }
        synchronized(writeLock) {
            try {
                file.parent?.let(Files::createDirectories)
                Files.writeString(file, "${Clock.System.now()}\n$message\n\n", CREATE, APPEND)
            } catch (_: IOException) {
                System.err.println("Could not append HTTP traffic log")
            } catch (_: SecurityException) {
                System.err.println("Could not append HTTP traffic log")
            }
        }
        Unit
    }

    companion object {
        private val writeLock = Any()
    }
}
