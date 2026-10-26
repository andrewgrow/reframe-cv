package reframecv.documents.vacancy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.text.PDFTextStripper

class VacancyPdfRendererTest {
    private val short = VacancyPrintData(
        "Senior Android Developer",
        "Example Systems",
        "https://example.test/jobs/123",
        "Build accessible mobile apps.\n\nWork with an international team: Grüße, équipe, команда.",
        listOf("Develop Kotlin applications", "Collaborate with designers"),
        listOf("Kotlin and Jetpack Compose", "Automated testing"),
        listOf("Design system experience"),
    )

    @Test
    fun singlePageIncludesSectionsUnicodeEmbeddedFontsAndClickableSource() {
        Loader.loadPDF(DesktopVacancyPdfRenderer.render(short)).use { pdf ->
            assertEquals(1, pdf.numberOfPages)
            assertCompleteText(short, pdf)
            val page = pdf.getPage(0)
            assertEquals(595f, page.mediaBox.width, 1f)
            assertEquals(842f, page.mediaBox.height, 1f)
            page.resources.fontNames.forEach { assertTrue(page.resources.getFont(it).isEmbedded) }
            val links = page.annotations.filterIsInstance<PDAnnotationLink>()
            assertEquals(listOf(short.url), links.map { (it.action as PDActionURI).uri })
            assertTrue(links.all { it.rectangle.width > 0 && it.rectangle.height > 0 })
            assertInsidePage(pdf)
        }
    }

    @Test
    fun minimalVacancyOmitsEmptySectionsAndDoesNotRequireACompanyOrUrl() {
        val data = VacancyPrintData("Example vacancy", responsibilities = listOf("", "  "))
        Loader.loadPDF(DesktopVacancyPdfRenderer.render(data)).use { pdf ->
            assertEquals("Example vacancy", PDFTextStripper().getText(pdf).trim())
            assertTrue(pdf.getPage(0).annotations.isEmpty())
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopVacancyPdfRenderer.render(VacancyPrintData(" "))
        }
    }

    @Test
    fun largeDescriptionAndOversizedBulletKeepEveryWordExactlyOnce() {
        val data = short.copy(
            description = (1..140).joinToString(" ") {
                "Description $it covers accessible Android applications and reliable delivery."
            },
            responsibilities = listOf(
                (1..100).joinToString(" ") {
                    "Responsibility $it includes technical design and automated testing."
                },
            ),
        )
        Loader.loadPDF(DesktopVacancyPdfRenderer.render(data)).use { pdf ->
            assertTrue(pdf.numberOfPages >= 3)
            assertCompleteText(data, pdf)
            assertInsidePage(pdf)
            repeat(pdf.numberOfPages) { index ->
                assertTrue("${index + 1} / ${pdf.numberOfPages}" in pageText(pdf, index))
            }
        }
    }

    @Test
    fun headingsNeverEndAPageWithoutTheirFollowingText() {
        val data = short.copy(
            description = (1..30).joinToString("\n\n") {
                "Paragraph $it. " +
                    "Build reliable applications with Kotlin and automated tests. ".repeat(4)
            },
        )
        Loader.loadPDF(DesktopVacancyPdfRenderer.render(data)).use { pdf ->
            assertCompleteText(data, pdf)
            repeat(pdf.numberOfPages) { index ->
                val text = bodyText(pdf, index).trim()
                listOf("Description", "Responsibilities", "Requirements", "Nice-to-have").forEach {
                    assertFalse(text.endsWith(it), "Orphan section heading on page ${index + 1}")
                }
            }
        }
    }

    private fun assertCompleteText(data: VacancyPrintData, pdf: PDDocument) {
        val actual = (0 until pdf.numberOfPages).joinToString(" ") { bodyText(pdf, it) }
        val expected = data.blocks().joinToString(" ") { it.text }
        fun normalize(value: String) = value.replace("•", "").replace(Regex("\\s+"), " ").trim()
        assertEquals(normalize(expected), normalize(actual))
    }
    private fun bodyText(pdf: PDDocument, index: Int) = pageText(pdf, index)
        .replace(Regex("(?m)^${index + 1} / ${pdf.numberOfPages}\\s*$"), "")
    private fun pageText(pdf: PDDocument, index: Int) = PDFTextStripper().apply {
        startPage = index + 1
        endPage = index + 1
    }.getText(pdf)
    private fun assertInsidePage(pdf: PDDocument) {
        object : PDFTextStripper() {
            override fun processTextPosition(text: org.apache.pdfbox.text.TextPosition) {
                assertTrue(text.xDirAdj >= 55)
                assertTrue(text.xDirAdj + text.widthDirAdj <= 541)
                assertTrue(text.yDirAdj in 55f..787f)
                super.processTextPosition(text)
            }
        }.getText(pdf)
    }
}
