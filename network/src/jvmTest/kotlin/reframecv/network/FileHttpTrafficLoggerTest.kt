package reframecv.network

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FileHttpTrafficLoggerTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

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
