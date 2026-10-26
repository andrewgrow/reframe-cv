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
import reframecv.domain.models.vacancy.Vacancy

class DesktopVacancyPdfExporterTest {
    @get:Rule val folder = TemporaryFolder()
    private val clock = Clock.fixed(Instant.parse("2026-10-26T12:34:56Z"), ZoneOffset.UTC)
    private val vacancy = Vacancy(
        id = 12, projectId = 7, name = "Internal name", createdAt = 0, updatedAt = 0,
        jobTitle = "Android Engineer", company = "Example Systems",
        url = "https://example.test/jobs/123",
        description = "Build accessible Kotlin applications.",
        responsibilities = listOf("Ship tested features"), requirements = listOf("Kotlin"),
        niceToHave = listOf("Compose"), keywords = listOf("Private keyword"), appliedAt = 123,
    )

    @Test
    fun generatesRealPdfBeforeDestinationSelectionAndExportsOnlyVacancyContent() = runBlocking {
        val temp = folder.newFolder("temp").toPath()
        val target = folder.newFile("vacancy.pdf").toPath()
        val exporter = DesktopVacancyPdfExporter(
            ExportDestinationPicker { name ->
                assertEquals("vacancy-12-20261026-1234-56.pdf", name)
                Files.list(temp).use { assertTrue(Files.size(it.toList().single()) > 0) }
                target.toString()
            },
            clock,
            temp,
        )
        assertEquals(DocumentExportResult.Saved, exporter.export(vacancy))
        Loader.loadPDF(target.toFile()).use { pdf ->
            val text = PDFTextStripper().getText(pdf)
            assertTrue("Android Engineer" in text && "Example Systems" in text)
            assertTrue(
                "Responsibilities" in text && "Requirements" in text && "Nice-to-have" in text,
            )
            assertFalse("Internal name" in text || "Private keyword" in text)
        }
        assertEmpty(temp)
        assertEquals("Internal name", vacancy.copy(jobTitle = "").toPrintData().title)
    }

    @Test
    fun cancellationAndRenderOrCopyFailureAlwaysRemoveTemporaryFiles() = runBlocking {
        val temp = folder.newFolder("temp").toPath()
        fun exporter(
            picker: ExportDestinationPicker,
            render: () -> ByteArray = { byteArrayOf(1) },
        ) = DesktopVacancyPdfExporter(picker, clock, temp) { render() }
        assertEquals(
            DocumentExportResult.Cancelled,
            exporter(ExportDestinationPicker { null }).export(vacancy),
        )
        assertEmpty(temp)
        assertFailsWith<IllegalStateException> {
            exporter(ExportDestinationPicker { error("Must not open") }) { error("Render failed") }
                .export(vacancy)
        }
        assertEmpty(temp)
        assertFailsWith<java.io.IOException> {
            exporter(
                ExportDestinationPicker {
                    folder.root.toPath().resolve("missing/vacancy.pdf").toString()
                },
            ).export(vacancy)
        }
        assertEmpty(temp)
    }

    @Test
    fun leavingWhilePickingADestinationCleansUpGeneratedPdf() = runBlocking {
        val temp = folder.newFolder("temp").toPath()
        val entered = CompletableDeferred<Unit>()
        val exporter = DesktopVacancyPdfExporter(
            ExportDestinationPicker {
                entered.complete(Unit)
                awaitCancellation()
            },
            clock,
            temp,
        ) { byteArrayOf(1) }
        val job = async { exporter.export(vacancy) }
        withTimeout(5000) { entered.await() }
        job.cancelAndJoin()
        assertEmpty(temp)
    }
    private fun assertEmpty(directory: Path) {
        Files.list(directory).use { assertEquals(0, it.count()) }
    }
}
