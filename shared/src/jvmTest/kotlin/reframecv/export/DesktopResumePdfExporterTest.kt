package reframecv.export

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.documents.resume.DesktopResumePdfRenderer
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeLanguagesSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.export.fixtures.hiddenResumeSections
import reframecv.export.fixtures.printableResumeFixture
import reframecv.export.fixtures.printableResumePortrait
import reframecv.export.fixtures.printableTextResumeFixture
import reframecv.photos.PhotoCrop
import reframecv.photos.ResumePhotos

class DesktopResumePdfExporterTest {
    @get:Rule val folder = TemporaryFolder()
    private val clock = Clock.fixed(Instant.parse("2026-10-21T12:34:56Z"), ZoneOffset.UTC)

    @Test
    fun exportsVisibleDataAndOnePortraitWithoutRepeatingTextAcrossPages() = runBlocking<Unit> {
        val destination = folder.newFile("resume.pdf").toPath()
        val temporary = folder.newFolder("temp").toPath()
        var reads = 0
        val photos = photos {
            reads++
            printableResumePortrait()
        }
        val exporter = DesktopResumePdfExporter(
            photos,
            ExportDestinationPicker {
                assertEquals("resume-12-20261021-1234-56.pdf", it)
                destination.toString()
            },
            clock,
            temporary,
        )
        assertEquals(ResumeExportResult.Saved, exporter.export(printableResumeFixture()))
        Loader.loadPDF(destination.toFile()).use { pdf ->
            assertTrue(pdf.numberOfPages > 1)
            val text = PDFTextStripper().getText(pdf)
            assertEquals(1, Regex("Alex Taylor").findAll(text).count())
            assertEquals(1, Regex("Field application 5").findAll(text).count())
            assertFalse("Hidden confidential text" in text)
            assertTrue("Graduated with honors" in text)
            val pages = (1..pdf.numberOfPages).map { page ->
                PDFTextStripper().apply {
                    startPage = page
                    endPage = page
                }.getText(pdf)
            }
            assertTrue(
                pages.any {
                    "Example University" in it && "2011 - 2015" in it &&
                        "Graduated with honors" in it
                },
            )
            assertEquals(12, pdf.pages.sumOf { it.annotations.size })
            val fonts = pdf.pages.flatMap { page ->
                page.resources.fontNames.map { page.resources.getFont(it) }
            }
            assertTrue(fonts.all { it.isEmbedded })
            assertTrue(fonts.any { "Tinos-Bold" in it.name })
            assertTrue(fonts.any { "Tinos-Italic" in it.name })
            val images = pdf.pages.map { page ->
                page.resources.xObjectNames.count {
                    page.resources.getXObject(it) is PDImageXObject
                }
            }
            assertEquals(1, images.first())
            assertTrue(images.drop(1).all { it == 0 })
        }
        assertEquals(1, reads)
        Files.list(temporary).use { assertEquals(0L, it.count()) }
        val report = Path.of("build/reports/pdf/resume.pdf")
        Files.createDirectories(report.parent)
        Files.copy(destination, report, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }

    @Test
    fun disabledPhotoIsNeverReadAndMissingPhotoDoesNotLeavePlaceholder() = runBlocking<Unit> {
        val destination = folder.newFile("resume.pdf").toPath()
        var reads = 0
        val exporter = DesktopResumePdfExporter(
            photos {
                reads++
                null
            },
            ExportDestinationPicker { destination.toString() },
            clock,
        )
        listOf(false, true).forEach { visible ->
            val original = printableResumeFixture()
            exporter.export(
                original.copy(
                    photoVisible = visible,
                    content = original.content.copy(
                        sections = hiddenResumeSections(),
                        consent = original.content.consent.copy(visible = false),
                    ),
                ),
            )
            Loader.loadPDF(destination.toFile()).use { pdf ->
                assertTrue(
                    pdf.pages.all { page ->
                        page.resources.xObjectNames.none {
                            page.resources.getXObject(it) is PDImageXObject
                        }
                    },
                )
            }
        }
        assertEquals(1, reads)
    }

    @Test
    fun textOnlyResumeUsesCompactContactsWithoutRepeatingAContactsSection() = runBlocking<Unit> {
        val destination = folder.newFile("text.pdf").toPath()
        val exporter = DesktopResumePdfExporter(
            photos { error("Hidden photo must not be read") },
            ExportDestinationPicker { destination.toString() },
            clock,
        )
        exporter.export(printableTextResumeFixture())
        Loader.loadPDF(destination.toFile()).use { pdf ->
            val text = PDFTextStripper().getText(pdf)
            assertEquals(1, Regex("alex@example.com").findAll(text).count())
            assertFalse("Contacts" in text)
            assertTrue(text.indexOf("alex@example.com") < text.indexOf("Senior Android Developer"))
        }
        val report = Path.of("build/reports/pdf/resume-text.pdf")
        Files.createDirectories(report.parent)
        Files.copy(destination, report, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }

    @Test
    fun headerAvoidsPortraitAndIndependentTextBlocksHaveVisibleSeparation() {
        val original = printableResumeFixture()
        val content = original.content.copy(
            profile = original.content.profile.copy(
                headline = "PROFILE Senior Android Developer Kotlin and Jetpack Compose delivery",
            ),
            sections = original.content.sections.mapNotNull {
                when (it) {
                    is ResumeSummarySection -> it.copy(text = "LEAD paragraph.\n\nBODY paragraph.")

                    is ResumeExperienceSection -> it.copy(
                        entries = listOf(
                            it.entries.first().copy(
                                achievements = listOf("ACHIEVEMENT"),
                                projects = emptyList(),
                            ),
                        ),
                    )

                    is ResumeLanguagesSection -> it.copy(
                        entries = it.entries.take(1),
                    )

                    else -> null
                }
            },
        )
        val data = original.copy(content = content).toPrintData(printableResumePortrait())
        Loader.loadPDF(DesktopResumePdfRenderer.render(data)).use { pdf ->
            val positions = mutableListOf<TextPosition>()
            object : PDFTextStripper() {
                override fun processTextPosition(position: TextPosition) {
                    positions.add(position)
                    super.processTextPosition(position)
                }
            }.getText(pdf)
            fun position(token: String): TextPosition = positions[
                positions.joinToString("") { it.unicode }.indexOf(token).also {
                    assertTrue(it >= 0, token)
                },
            ]
            assertTrue(position("BODY").yDirAdj - position("LEAD").yDirAdj >= 19)
            assertTrue(position("Application").yDirAdj - position("ACHIEVEMENT").yDirAdj >= 18)
            assertTrue(position("I consent").yDirAdj - position("English").yDirAdj >= 36)
            val headline = positions.filter {
                it.yDirAdj >= position("PROFILE").yDirAdj &&
                    it.yDirAdj < position("Professional").yDirAdj
            }
            assertTrue(headline.isNotEmpty())
            assertTrue(headline.all { it.xDirAdj + it.widthDirAdj <= 440 })
            assertTrue(position("PROFILE").yDirAdj > position("Remote").yDirAdj)
        }
    }

    private fun photos(read: () -> ByteArray?) = object : ResumePhotos {
        override suspend fun choose(): String? = null
        override suspend fun copySource(path: String): String = error("Unused")
        override suspend fun render(path: String, crop: PhotoCrop): String = error("Unused")
        override suspend fun read(path: String) = read()
        override suspend fun discard(path: String) = Unit
    }
}
