package reframecv.documents.coverletter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.text.PDFTextStripper
import reframecv.documents.PrintLink
import reframecv.documents.coverletter.fixtures.CoverLetterFixtures

class CoverLetterPdfRendererTest {
    @Test
    fun shortLetterKeepsAllTextUnicodeAndEmbeddedFonts() {
        val bytes = DesktopCoverLetterPdfRenderer.render(CoverLetterFixtures.short)
        Loader.loadPDF(bytes).use { document ->
            assertEquals(1, document.numberOfPages)
            val text = PDFTextStripper().getText(document)
            val expected = CoverLetterFixtures.short.text + "\n" +
                CoverLetterFixtures.links.joinToString("\n") { it.label }
            assertEquals(normalize(expected), normalize(text))
            val fonts = document.getPage(0).resources
            assertTrue(fonts.fontNames.toList().isNotEmpty())
            fonts.fontNames.forEach { assertTrue(fonts.getFont(it).isEmbedded) }
        }
    }

    @Test
    fun linksHaveUriActionsAndClickableRegionsInsideThePage() {
        val bytes = DesktopCoverLetterPdfRenderer.render(CoverLetterFixtures.short)
        Loader.loadPDF(bytes).use { document ->
            val page = document.getPage(0)
            val links = page.annotations.filterIsInstance<PDAnnotationLink>()
            assertEquals(
                CoverLetterFixtures.links.map { it.url },
                links.map {
                    (it.action as PDActionURI).uri
                },
            )
            links.forEach {
                val rectangle = it.rectangle
                assertTrue(rectangle.width > 0 && rectangle.height > 0)
                assertTrue(rectangle.lowerLeftX >= 0 && rectangle.lowerLeftY >= 0)
                assertTrue(rectangle.upperRightX <= page.mediaBox.width)
                assertTrue(rectangle.upperRightY <= page.mediaBox.height)
            }
        }
    }

    @Test
    fun paginationKeepsEachParagraphExactlyOnceInReadingOrder() {
        val data = CoverLetterFixtures.long
        Loader.loadPDF(DesktopCoverLetterPdfRenderer.render(data)).use { document ->
            assertEquals(3, document.numberOfPages)
            assertCompleteText(data, document)
            repeat(document.numberOfPages) { index ->
                assertTrue("${index + 1} / ${document.numberOfPages}" in pageText(document, index))
            }
            val links = document.pages.flatMap {
                it.annotations.filterIsInstance<PDAnnotationLink>()
            }
            assertEquals(data.links.map { it.url }, links.map { (it.action as PDActionURI).uri })
        }
    }

    @Test
    fun paragraphTallerThanPageSplitsWithoutLosingOrRepeatingWords() {
        val text = (1..250).joinToString(" ") {
            "Sentence $it demonstrates practical Kotlin development and careful testing."
        }
        val data = CoverLetterPrintData(text, CoverLetterFixtures.links)
        Loader.loadPDF(DesktopCoverLetterPdfRenderer.render(data)).use { document ->
            assertTrue(document.numberOfPages > 2)
            assertCompleteText(data, document)
            assertTextInsideMargins(document)
        }
    }

    @Test
    fun explicitLineBreaksAndLongLinkLabelsSurvivePageBoundaries() {
        val text = (1..120).joinToString("\n") { "Line $it: Grüße, équipe, команда." }
        val data = CoverLetterPrintData(
            text,
            listOf(PrintLink("Portfolio ".repeat(180).trim(), "https://example.com")),
        )
        Loader.loadPDF(DesktopCoverLetterPdfRenderer.render(data)).use { document ->
            assertCompleteText(data, document)
            assertTextInsideMargins(document)
            assertTrue(document.pages.flatMap { it.annotations }.isNotEmpty())
        }
    }

    private fun assertCompleteText(data: CoverLetterPrintData, document: PDDocument) {
        val actual = (0 until document.numberOfPages).joinToString(" ") { index ->
            pageText(document, index).replace(
                Regex("(?m)^${index + 1} / ${document.numberOfPages}\\s*$"),
                "",
            )
        }
        val expected = data.text + " " + data.links.joinToString(" ") { it.label }
        assertEquals(normalize(expected), normalize(actual))
    }

    private fun assertTextInsideMargins(document: PDDocument) {
        object : PDFTextStripper() {
            override fun processTextPosition(text: org.apache.pdfbox.text.TextPosition) {
                assertTrue(text.xDirAdj >= 55, "Text crosses left margin")
                assertTrue(text.xDirAdj + text.widthDirAdj <= 540, "Text crosses right margin")
                assertTrue(text.yDirAdj >= 55, "Text crosses top margin")
                assertTrue(text.yDirAdj <= 787, "Text crosses bottom margin")
                super.processTextPosition(text)
            }
        }.getText(document)
    }

    @Test
    fun blankTextAndBlankLinkTargetsAreRejectedBeforeRendering() {
        assertFailsWith<IllegalArgumentException> {
            DesktopCoverLetterPdfRenderer.render(CoverLetterPrintData("  "))
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopCoverLetterPdfRenderer.render(
                CoverLetterPrintData("Text", listOf(PrintLink("URL", ""))),
            )
        }
    }

    private fun pageText(document: PDDocument, index: Int) = PDFTextStripper().apply {
        startPage = index + 1
        endPage = index + 1
    }.getText(document)

    private fun normalize(text: String) = text.replace(Regex("\\s+"), " ").trim()
}
