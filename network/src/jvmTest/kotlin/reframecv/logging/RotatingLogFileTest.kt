package reframecv.logging

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class RotatingLogFileTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun reopeningAppendsAndRotationKeepsWholeEntriesMeasuredInUtf8Bytes() {
        val file = temporaryFolder.root.toPath().resolve("logs/traffic.log")
        RotatingLogFile(file, maxFileBytes = 16).append("é".repeat(4))
        RotatingLogFile(file, maxFileBytes = 16).append("é".repeat(4))
        assertEquals("é".repeat(8), Files.readString(file))
        assertFalse(Files.exists(file.resolveSibling("traffic.log.1")))

        RotatingLogFile(file, maxFileBytes = 16).append("next\n")
        assertEquals("é".repeat(8), Files.readString(file.resolveSibling("traffic.log.1")))
        assertEquals("next\n", Files.readString(file))
    }

    @Test
    fun keepsFiveArchivesInOrderAndRemovesOnlyTheOldest() {
        val file = temporaryFolder.root.toPath().resolve("stacktrace.txt")
        (1..8).forEach { RotatingLogFile(file, maxFileBytes = 8).append("event0$it\n") }
        assertEquals("event08\n", Files.readString(file))
        (1..5).forEach { index ->
            assertEquals(
                "event0${8 - index}\n",
                Files.readString(file.resolveSibling("stacktrace.txt.$index")),
            )
        }
        assertFalse(Files.exists(file.resolveSibling("stacktrace.txt.6")))
        assertEquals(6, files().size)
        assertTrue(files().all { Files.size(it) <= 8 })
    }

    @Test
    fun splitsOversizedEntriesWithoutBreakingUtf8OrExceedingTheLimit() {
        val file = temporaryFolder.root.toPath().resolve("traffic.log")
        val entry = "Я🙂".repeat(5)
        RotatingLogFile(file, maxFileBytes = 7).append(entry)
        val ordered = (5 downTo 1).map { file.resolveSibling("traffic.log.$it") }
            .filter(Files::exists) + listOf(file)
        val text = ordered.joinToString("") {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(it))).toString()
        }
        assertEquals(entry, text)
        assertTrue(files().all { Files.size(it) <= 7 })
    }

    @Test
    fun evenOneHugeEntryRetainsOnlyTheMostRecentSixFiles() {
        val file = temporaryFolder.root.toPath().resolve("traffic.log")
        val entry = (0..9).joinToString("") { "chunk0$it\n" }
        RotatingLogFile(file, maxFileBytes = 8).append(entry)
        val ordered = (5 downTo 1).map { file.resolveSibling("traffic.log.$it") } + listOf(file)
        assertEquals(entry.takeLast(48), ordered.joinToString("") { Files.readString(it) })
        assertEquals(6, files().size)
        assertTrue(files().all { Files.size(it) <= 8 })
    }

    @Test
    fun boundsExistingOversizedLogsKeepingTheirMostRecentText() {
        val file = temporaryFolder.root.toPath().resolve("traffic.log")
        Files.writeString(file, "old".repeat(100) + "🙂Я".repeat(5) + "RECENT")
        RotatingLogFile(file, maxFileBytes = 16).append("next\n")
        val archive = file.resolveSibling("traffic.log.1")
        val text = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(archive)))
        assertTrue(text.toString().endsWith("RECENT"))
        assertEquals("next\n", Files.readString(file))
        assertTrue(files().all { Files.size(it) <= 16 })
        assertEquals(2, files().size)
    }

    @Test
    fun concurrentInstancesSerializeAppendsAndRotationWithoutLosingRetainedEntries() {
        val file = temporaryFolder.root.toPath().resolve("traffic.log")
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..100).map { id ->
                executor.submit {
                    RotatingLogFile(
                        file,
                        maxFileBytes = 128,
                        archiveCount = 20,
                    ).append("event$id\n")
                }
            }
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
        val lines = files().flatMap { Files.readAllLines(it) }
        assertEquals((1..100).map { "event$it" }.sorted(), lines.sorted())
        assertTrue(files().all { Files.size(it) <= 128 })
    }

    private fun files(): List<Path> = Files.list(temporaryFolder.root.toPath()).use { it.toList() }
}
