package reframecv.documents.resume

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import reframecv.documents.PrintLink

class ResumePdfRendererTest {
    @Test
    fun oversizedParagraphKeepsAllWordsExactlyOnceWithinPageMargins() {
        val body = (1..300).joinToString(" ") { "Sentence $it describes reliable Kotlin delivery." }
        val data = ResumePrintData(
            listOf(ResumePrintItem("Alex Taylor", ResumePrintKind.Name)),
            listOf(ResumePrintItem(body)),
        )
        Loader.loadPDF(DesktopResumePdfRenderer.render(data)).use { pdf ->
            assertTrue(pdf.numberOfPages > 2)
            val text = PDFTextStripper().getText(pdf).replace(Regex("(?m)^\\d+ / \\d+\\s*$"), "")
            assertEquals(normalize("Alex Taylor $body"), normalize(text))
            object : PDFTextStripper() {
                override fun processTextPosition(position: org.apache.pdfbox.text.TextPosition) {
                    assertTrue(position.xDirAdj >= 84)
                    assertTrue(position.xDirAdj + position.widthDirAdj <= 554)
                    assertTrue(position.yDirAdj in 55f..787f)
                    super.processTextPosition(position)
                }
            }.getText(pdf)
        }
    }

    @Test
    fun sectionAndPositionHeadingsStayWithTheOpeningBodyLine() {
        val items = (1..35).map { ResumePrintItem("Previous entry $it") } + listOf(
            ResumePrintItem("Experience heading", ResumePrintKind.Section, keepWithNext = true),
            ResumePrintItem("Position heading", ResumePrintKind.Subheading, keepWithNext = true),
            ResumePrintItem("Opening body line"),
        )
        Loader.loadPDF(
            DesktopResumePdfRenderer.render(ResumePrintData(emptyList(), items)),
        ).use { pdf ->
            val pages = (1..pdf.numberOfPages).map { page ->
                PDFTextStripper().apply {
                    startPage = page
                    endPage = page
                }.getText(pdf)
            }
            val headingPage = pages.single { "Experience heading" in it }
            assertTrue("Position heading" in headingPage)
            assertTrue("Opening body line" in headingPage)
        }
    }

    @Test
    fun longContactStripWrapsWithoutClippingOrLosingLinks() {
        val contacts = (1..6).map {
            PrintLink("Contact $it alexander.taylor@example.com", "mailto:alexander$it@example.com")
        }
        val data = ResumePrintData(
            listOf(ResumePrintItem("Алекс Тейлор Grüße", ResumePrintKind.Name)),
            listOf(ResumePrintItem("Body starts after contacts")),
            contacts = contacts,
        )
        Loader.loadPDF(DesktopResumePdfRenderer.render(data)).use { pdf ->
            val text = normalize(PDFTextStripper().getText(pdf))
            contacts.forEach { assertTrue(it.label in text) }
            assertTrue("Алекс Тейлор Grüße" in text)
            assertEquals(contacts.size, pdf.getPage(0).annotations.size)
            assertTrue(text.indexOf("Contact 6") < text.indexOf("Body starts"))
        }
    }

    @Test
    fun oversizedBulletAndInlineEmphasisPreserveEveryWordAcrossPages() {
        val body = (1..400).joinToString(" ") { "Achievement $it improved release reliability." }
        val item = ResumePrintItem(body, ResumePrintKind.Bullet, boldPrefixLength = 20)
        Loader.loadPDF(
            DesktopResumePdfRenderer.render(ResumePrintData(emptyList(), listOf(item))),
        ).use { pdf ->
            val text = PDFTextStripper().getText(pdf)
                .replace(Regex("(?m)^\\d+ / \\d+\\s*$"), "").replace("•", "")
            assertEquals(normalize(body), normalize(text))
        }
    }

    private fun normalize(text: String) = text.replace(Regex("\\s+"), " ").trim()
}
