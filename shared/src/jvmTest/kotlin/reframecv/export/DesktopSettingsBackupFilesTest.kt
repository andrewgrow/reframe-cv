package reframecv.export

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.importing.JsonImportPicker

class DesktopSettingsBackupFilesTest {
    @get:Rule val folder = TemporaryFolder()
    private val clock = Clock.fixed(Instant.parse("2026-10-21T12:34:56Z"), ZoneOffset.UTC)

    @Test
    fun nativeFileFlowPreservesSourceAndCleansExportTemporaryFiles() = runBlocking<Unit> {
        val source = folder.newFile("source.json").toPath()
        Files.writeString(source, "{\"example\":true}")
        val destination = folder.newFile("saved.json").toPath()
        val temporary = folder.newFolder("temp").toPath()
        val files = DesktopSettingsBackupFiles(
            JsonImportPicker { source.toString() },
            ExportDestinationPicker {
                assertEquals("settings-20261021-1234-56.json", it)
                Files.list(temporary).use { entries -> assertEquals(1L, entries.count()) }
                destination.toString()
            },
            clock,
            temporary,
        )
        assertEquals("{\"example\":true}", files.read())
        assertEquals(DocumentExportResult.Saved, files.write("exported settings"))
        assertEquals("exported settings", Files.readString(destination))
        assertEquals("{\"example\":true}", Files.readString(source))
        Files.list(temporary).use { assertEquals(0L, it.count()) }
    }

    @Test
    fun cancelledAndFailedFileOperationsDoNotLeaveTemporaryCopies() = runBlocking<Unit> {
        val temporary = folder.newFolder("temp").toPath()
        val files =
            DesktopSettingsBackupFiles(
                JsonImportPicker {
                    null
                },
                ExportDestinationPicker { null },
                clock,
                temporary,
            )
        assertNull(files.read())
        assertEquals(DocumentExportResult.Cancelled, files.write("private"))
        val failing = DesktopSettingsBackupFiles(
            destination = ExportDestinationPicker { folder.root.resolve("missing/file.json").path },
            temporaryDirectory = temporary,
        )
        assertFailsWith<java.io.IOException> { failing.write("private") }
        Files.list(temporary).use { assertEquals(0L, it.count()) }
    }

    @Test
    fun unreadableAndOversizedFilesAreRejected() = runBlocking<Unit> {
        val source = folder.newFile("large.json").toPath()
        Files.write(source, ByteArray(10 * 1024 * 1024 + 1))
        val files = DesktopSettingsBackupFiles(JsonImportPicker { source.toString() })
        assertFailsWith<IllegalArgumentException> { files.read() }
        Files.write(source, byteArrayOf(0xFF.toByte()))
        assertFailsWith<java.nio.charset.CharacterCodingException> { files.read() }
    }
}
