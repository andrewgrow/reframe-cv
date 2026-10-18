package reframecv.network

import io.klogging.noCoLogger
import java.io.IOException
import java.nio.file.Path
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import reframecv.logging.RotatingLogFile

/** Writes sanitized HTTP diagnostics to the console and a file without blocking the UI thread. */
class FileHttpTrafficLogger(file: Path) : HttpTrafficLogger {
    private val console = noCoLogger("OpenRouterHttp")
    private val output = RotatingLogFile(file)

    override suspend fun write(message: String) = withContext(Dispatchers.IO) {
        runCatching { console.info("{exchange}", message) }
        try {
            output.append("${Clock.System.now()}\n$message\n\n")
        } catch (_: IOException) {
            System.err.println("Could not append HTTP traffic log")
        } catch (_: SecurityException) {
            System.err.println("Could not append HTTP traffic log")
        }
        Unit
    }
}
