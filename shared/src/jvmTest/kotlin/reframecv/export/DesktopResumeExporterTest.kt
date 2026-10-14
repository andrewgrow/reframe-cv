package reframecv.export

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.resume.ResumeContentJson
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeConsent
import reframecv.domain.models.resume.content.sampleResumeContent

class DesktopResumeExporterTest {
    @get:Rule val folder = TemporaryFolder()
    private val clock = Clock.fixed(Instant.parse("2026-10-14T12:34:56Z"), ZoneOffset.UTC)
    private val resume = Resume(
        id = 12,
        projectId = 7,
        name = "Mobile",
        content = sampleResumeContent().copy(
            language = "RU",
            consent = ResumeConsent(text = "Consent\nText", visible = false),
        ),
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun generatesDocumentBeforePickerAndSavesOriginalContentWithSuggestedFilename() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val destination = folder.newFile("chosen.json").toPath()
        val exporter = DesktopResumeExporter(
            ExportDestinationPicker { name ->
                assertEquals("resume-12-20261410-1234-56.json", name)
                assertReady(temporary)
                destination.toString()
            },
            clock,
            temporary,
        )
        assertEquals(ResumeExportResult.Saved, exporter.export(resume))
        val exported = Files.readString(destination)
        assertTrue(exported.startsWith("{\n    \"formatVersion\": 1,"))
        assertTrue(exported.endsWith("\n"))
        assertEquals(
            Json.parseToJsonElement(ResumeContentJson.encode(resume.content)),
            Json.parseToJsonElement(exported),
        )
        assertEquals(resume.content, ResumeContentJson.decode(exported))
        assertEmpty(temporary)
    }

    @Test
    fun cancellingPickerDeletesGeneratedTemporaryDocument() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val exporter = DesktopResumeExporter(
            ExportDestinationPicker {
                assertReady(temporary)
                null
            },
            clock,
            temporary,
        )
        assertEquals(ResumeExportResult.Cancelled, exporter.export(resume))
        assertEmpty(temporary)
    }

    @Test
    fun pickerAndCopyFailuresDeleteTemporaryDocument() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val brokenPicker = DesktopResumeExporter(
            ExportDestinationPicker { error("Picker unavailable") },
            clock,
            temporary,
        )
        assertFailsWith<IllegalStateException> { brokenPicker.export(resume) }
        assertEmpty(temporary)
        val brokenDestination = DesktopResumeExporter(
            ExportDestinationPicker {
                folder.root.toPath().resolve("missing/resume.json").toString()
            },
            clock,
            temporary,
        )
        assertFailsWith<java.io.IOException> { brokenDestination.export(resume) }
        assertEmpty(temporary)
    }

    @Test
    fun coroutineCancellationStillDeletesTemporaryDocument() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val entered = CompletableDeferred<Unit>()
        val exporter = DesktopResumeExporter(
            ExportDestinationPicker {
                assertReady(temporary)
                entered.complete(Unit)
                awaitCancellation()
            },
            clock,
            temporary,
        )
        val operation = async { exporter.export(resume) }
        withTimeout(10_000) {
            entered.await()
            operation.cancelAndJoin()
        }
        assertEmpty(temporary)
    }

    private fun assertReady(directory: Path) {
        Files.list(directory).use { files ->
            val temporary = files.toList().single()
            assertTrue(temporary.fileName.toString().endsWith(".json"))
            assertEquals(resume.content, ResumeContentJson.decode(Files.readString(temporary)))
        }
    }

    private fun assertEmpty(directory: Path) {
        Files.list(directory).use { assertEquals(0L, it.count()) }
    }
}
