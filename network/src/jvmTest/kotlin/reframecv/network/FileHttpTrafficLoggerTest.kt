package reframecv.network

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.logging.RotatingLogFile

class FileHttpTrafficLoggerTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun rotatesFullHttpLogsAndPreservesResponsesAcrossInstances() = runBlocking<Unit> {
        val path = temporaryFolder.root.toPath().resolve("openrouter-http.log")
        Files.writeString(path, "x".repeat(RotatingLogFile.DEFAULT_MAX_FILE_BYTES))
        FileHttpTrafficLogger(path).write("RESPONSE 200 GET /models/user")
        FileHttpTrafficLogger(path).write("REQUEST GET /key")
        assertEquals(
            RotatingLogFile.DEFAULT_MAX_FILE_BYTES.toLong(),
            Files.size(path.resolveSibling("openrouter-http.log.1")),
        )
        val text = Files.readString(path)
        assertTrue(text.contains("RESPONSE 200 GET /models/user"))
        assertTrue(text.contains("REQUEST GET /key"))
    }

    @Test
    fun createsTheLogAndAppendsExchangesWithTimestamps() = runBlocking<Unit> {
        val path = temporaryFolder.root.toPath().resolve("logs/openrouter-http.log")
        val logger = FileHttpTrafficLogger(path)
        logger.write("REQUEST GET /key")
        logger.write("RESPONSE 200 GET /key")
        val content = Files.readString(path)
        assertTrue(content.indexOf("REQUEST") < content.indexOf("RESPONSE"))
        assertTrue(content.contains("T"))
    }
}
