package reframecv.export

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.domain.models.coverletter.CoverLetter

class DesktopCoverLetterPdfExporterTest {
    @get:Rule val folder = TemporaryFolder()
    private val clock = Clock.fixed(Instant.parse("2026-10-21T12:34:56Z"), ZoneOffset.UTC)
    private val letter = CoverLetter(
        12,
        7,
        "Internal name",
        0,
        0,
        content = "Dear team,\n\nI build Kotlin apps.\n\nAlex",
        keywords = listOf("Internal keyword"),
    )

    @Test
    fun rendersRealPdfBeforePickerAndSavesOnlyPrintableText() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val destination = folder.newFile("letter.pdf").toPath()
        val exporter = DesktopCoverLetterPdfExporter(
            ExportDestinationPicker { name ->
                assertEquals("cover-letter-12-20261021-1234-56.pdf", name)
                Files.list(temporary).use { paths ->
                    val file = paths.toList().single()
                    assertTrue(file.toString().endsWith(".pdf"))
                    assertTrue(Files.size(file) > 0)
                }
                destination.toString()
            },
            clock,
            temporary,
        )
        assertEquals(DocumentExportResult.Saved, exporter.export(letter))
        Loader.loadPDF(destination.toFile()).use { document ->
            assertEquals(1, document.numberOfPages)
            val text = PDFTextStripper().getText(document)
            assertTrue("Dear team," in text && "Kotlin apps." in text)
            assertFalse("Internal name" in text || "Internal keyword" in text)
        }
        assertEmpty(temporary)
    }

    @Test
    fun pickerCancellationAndRenderOrCopyFailureCleanUpTemporaryPdf() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        fun exporter(
            picker: ExportDestinationPicker,
            render: () -> ByteArray = { byteArrayOf(1) },
        ) = DesktopCoverLetterPdfExporter(picker, clock, temporary) { render() }
        assertEquals(
            DocumentExportResult.Cancelled,
            exporter(ExportDestinationPicker { null }).export(letter),
        )
        assertEmpty(temporary)
        assertFailsWith<IllegalStateException> {
            exporter(ExportDestinationPicker { error("Must not open") }) {
                error("Render failed")
            }.export(letter)
        }
        assertEmpty(temporary)
        assertFailsWith<java.io.IOException> {
            exporter(
                ExportDestinationPicker {
                    folder.root.toPath().resolve("missing/letter.pdf").toString()
                },
            ).export(letter)
        }
        assertEmpty(temporary)
    }

    @Test
    fun coroutineCancellationCleansUpPendingPdf() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val entered = CompletableDeferred<Unit>()
        val exporter = DesktopCoverLetterPdfExporter(
            ExportDestinationPicker {
                entered.complete(Unit)
                awaitCancellation()
            },
            clock,
            temporary,
            render = { byteArrayOf(1) },
        )
        val operation = async { exporter.export(letter) }
        withTimeout(5000) {
            entered.await()
            operation.cancelAndJoin()
        }
        assertEmpty(temporary)
    }

    private fun assertEmpty(directory: Path) {
        Files.list(directory).use { assertEquals(0L, it.count()) }
    }
}
