package reframecv.logging

import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FileApplicationLoggerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun appendsAcrossInstancesWithContextOriginalStackAndCause() {
        val file = temporaryFolder.root.toPath().resolve("data/stacktrace.txt")
        val cause = IllegalArgumentException("Invalid record")
        val error = IllegalStateException("Write failed", cause)
        FileApplicationLogger(file).error("Update vacancy", error, mapOf("vacancyId" to 42L))
        val first = Files.readString(file)
        FileApplicationLogger(file).error("Load projects", error, mapOf("parentId" to null))
        val text = Files.readString(file)
        assertTrue(text.startsWith(first))
        assertContains(text, "ERROR Update vacancy {vacancyId=42}")
        assertContains(text, "ERROR Load projects {parentId=null}")
        assertContains(text, error.stackTraceToString())
        assertContains(text, "Caused by: java.lang.IllegalArgumentException: Invalid record")
        assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}T").containsMatchIn(text))
    }

    @Test
    fun includesHistoryBeforeTheStackWithoutClearingItAfterAnError() {
        val history = EventHistory()
        val file = temporaryFolder.root.toPath().resolve("history/stacktrace.txt")
        val logger = FileApplicationLogger(file, history)
        (1..205).forEach { history.record("Event $it") }
        logger.error("Load dashboard", IllegalStateException("First failure"), emptyMap())
        val first = Files.readString(file)
        assertContains(first, "Recent events (200):")
        assertTrue(first.indexOf("Event 6\n") < first.indexOf("Event 205\n"))
        assertTrue(first.indexOf("Event 205\n") < first.indexOf("Stack trace:"))
        assertTrue(first.indexOf("Stack trace:") < first.indexOf("First failure"))
        assertTrue(!first.contains("Event 5\n"))
        history.record("ProjectTree OpenProjectsList")
        logger.error("Load projects", IllegalStateException("Second failure"), emptyMap())
        val text = Files.readString(file)
        assertTrue(text.startsWith(first))
        assertContains(text.substring(first.length), "ProjectTree OpenProjectsList")
        assertEquals(200, history.snapshot().size)
    }

    @Test
    fun concurrentFailuresRemainCompleteEntries() {
        val file = temporaryFolder.root.toPath().resolve("stacktrace.txt")
        val logger = FileApplicationLogger(file)
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..20).map { id ->
                executor.submit {
                    logger.error(
                        "Delete vacancy",
                        IllegalStateException("Failure $id"),
                        mapOf("vacancyId" to id),
                    )
                }
            }
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
        val entries = Files.readString(file).trim().split("\n\n")
        assertEquals(20, entries.size)
        (1..20).forEach { id ->
            val entry = entries.single { it.contains("{vacancyId=$id}") }
            assertContains(entry, "java.lang.IllegalStateException: Failure $id\n")
        }
    }

    @Test
    fun unwritableLogDoesNotReplaceTheApplicationFailure() {
        val directory = temporaryFolder.newFolder("not-a-file").toPath()
        FileApplicationLogger(directory).error(
            "Load dashboard",
            IllegalStateException("Read failed"),
            mapOf("projectId" to 1L),
        )
        assertTrue(Files.isDirectory(directory))
    }
}
