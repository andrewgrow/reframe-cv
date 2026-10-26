package reframecv.export

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeProfile
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.vacancy.Vacancy
import reframecv.export.fixtures.printableResumePortrait
import reframecv.photos.ResumePhotos
import reframecv.testing.ComponentTest

class DesktopVacancyArchiveExporterTest : ComponentTest() {
    @get:Rule val folder = TemporaryFolder()
    private val vacancy = Vacancy(7, 1, "Internal vacancy", 0, 0, jobTitle = "Android Engineer")
    private val resume = Resume(
        1,
        1,
        "Internal resume",
        0,
        0,
        content = ResumeContent(
            profile = ResumeProfile(name = "Alex Taylor"),
            sections = listOf(
                ResumeSummarySection(text = "Visible experience"),
                ResumeSummarySection(text = "Hidden experience", visible = false),
            ),
        ),
        photoPath = "portrait",
    )
    private val letter = CoverLetter(1, 1, "Internal letter", 0, 0, content = "Dear team,\n\nAlex")

    @Test
    fun zipContainsExactlyNamedRealPdfsAndUsesResumeVisibilityAndPortraitRules() = runBlocking {
        val temp = folder.newFolder("temp").toPath()
        val destination = folder.newFile("bundle.zip").toPath()
        val reads = mutableListOf<String>()
        val photos = object : ResumePhotos by dependencies.resumePhotos {
            override suspend fun read(path: String): ByteArray {
                reads += path
                return printableResumePortrait()
            }
        }
        val exporter = DesktopVacancyArchiveExporter(
            photos,
            ExportDestinationPicker { name ->
                assertEquals("Example - роль.zip", name)
                Files.list(temp).use { assertTrue(Files.size(it.toList().single()) > 0) }
                destination.toString()
            },
            temporaryDirectory = temp,
        )
        val bundle = VacancyArchive(
            vacancy,
            "Example / роль",
            listOf(resume.copy(photoVisible = false), resume.copy(id = 2)),
            listOf(letter, letter.copy(id = 2, content = "Second letter")),
        )
        assertEquals(DocumentExportResult.Saved, exporter.export(bundle))
        val entries = entries(Files.readAllBytes(destination))
        assertEquals(
            listOf(
                "Example - роль-vacancy.pdf",
                "Example - роль-resume-1.pdf",
                "Example - роль-resume-2.pdf",
                "Example - роль-coverLetter-1.pdf",
                "Example - роль-coverLetter-2.pdf",
            ),
            entries.keys.toList(),
        )
        assertEquals(listOf("portrait"), reads)
        entries.forEach { (name, bytes) ->
            Loader.loadPDF(bytes).use { pdf ->
                val text = PDFTextStripper().getText(pdf)
                when {
                    "-vacancy" in name -> assertTrue("Android Engineer" in text)

                    "-resume" in name -> {
                        assertTrue("Alex Taylor" in text && "Visible experience" in text)
                        assertFalse("Hidden experience" in text)
                        val images = pdf.pages.sumOf { page ->
                            page.resources.xObjectNames.count {
                                page.resources.getXObject(it) is PDImageXObject
                            }
                        }
                        assertEquals(if ("resume-2" in name) 1 else 0, images)
                    }

                    else -> assertTrue("letter" in text || "Dear team" in text)
                }
            }
        }
        assertEmpty(temp)
    }

    @Test
    fun cancelAndRenderingFailureRemoveTheTemporaryArchive() = runBlocking {
        val temp = folder.newFolder("temp").toPath()
        val exporter = DesktopVacancyArchiveExporter(
            dependencies.resumePhotos,
            ExportDestinationPicker { null },
            temporaryDirectory = temp,
        )
        val bundle = VacancyArchive(vacancy, "Job", emptyList(), emptyList())
        assertEquals(DocumentExportResult.Cancelled, exporter.export(bundle))
        assertEmpty(temp)
        assertFailsWith<IllegalArgumentException> {
            exporter.export(bundle.copy(coverLetters = listOf(letter.copy(content = " "))))
        }
        assertEmpty(temp)
    }

    @Test
    fun cancellationDuringDestinationSelectionCleansUpTheArchive() = runBlocking {
        val temp = folder.newFolder("temp").toPath()
        val entered = CompletableDeferred<Unit>()
        val exporter = DesktopVacancyArchiveExporter(
            dependencies.resumePhotos,
            ExportDestinationPicker {
                entered.complete(Unit)
                awaitCancellation()
            },
            temporaryDirectory = temp,
        )
        val task =
            async { exporter.export(VacancyArchive(vacancy, "Job", emptyList(), emptyList())) }
        withTimeout(5000) { entered.await() }
        task.cancelAndJoin()
        assertEmpty(temp)
    }
    private fun entries(bytes: ByteArray): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                assertFalse(entry.isDirectory)
                entries[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }
        return entries
    }
    private fun assertEmpty(path: Path) {
        Files.list(path).use { assertEquals(0, it.count()) }
    }
}
