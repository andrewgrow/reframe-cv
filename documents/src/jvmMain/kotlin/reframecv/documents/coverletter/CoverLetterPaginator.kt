package reframecv.documents.coverletter

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.chrisjenx.compose2pdf.PdfPageConfig

/** Splits measured Compose text before rendering, so off-page text never enters the PDF. */
internal class CoverLetterPaginator(config: PdfPageConfig) {
    private val density = CoverLetterPrintStyle.density
    private val width = with(density) { config.contentWidth.roundToPx() }
    private val height = with(density) {
        (config.contentHeight - CoverLetterPrintStyle.footerHeight).roundToPx()
    }
    private val measurer = TextMeasurer(
        createFontFamilyResolver(),
        density,
        LayoutDirection.Ltr,
    )
    private val pages = mutableListOf<MutableList<CoverLetterPrintBlock>>(mutableListOf())
    private var remaining = height

    fun paginate(data: CoverLetterPrintData): List<List<CoverLetterPrintBlock>> {
        val paragraphs = data.text.replace("\r\n", "\n").split(Regex("\n[\t ]*\n"))
            .filter(String::isNotBlank).map { CoverLetterPrintBlock(it.trim()) }
        val links = data.links.map { CoverLetterPrintBlock(it.label, it.url) }
        (paragraphs + links).forEach(::append)
        return pages
    }

    private fun measure(text: String) = measurer.measure(
        text,
        CoverLetterPrintStyle.body,
        constraints = Constraints(maxWidth = width),
    )

    private fun append(block: CoverLetterPrintBlock) {
        var rest = block
        while (rest.text.isNotEmpty()) {
            val layout = measure(rest.text)
            if (layout.size.height <= height && layout.size.height > remaining) newPage()
            if (layout.size.height <= remaining) {
                add(rest, layout.size.height)
                rest = rest.copy(text = "")
            } else {
                val end = fittingEnd(rest.text)
                if (end == 0) {
                    check(pages.last().isNotEmpty()) { "A text line exceeds the printable height" }
                    newPage()
                } else {
                    val fragment = rest.copy(
                        text = rest.text.substring(0, end),
                        paragraphEnd = false,
                    )
                    add(fragment, measure(fragment.text).size.height)
                    rest = rest.copy(text = rest.text.substring(end))
                    newPage()
                }
            }
        }
    }

    private fun fittingEnd(text: String): Int {
        val layout = measure(text)
        var lineCount = (0 until layout.lineCount).takeWhile {
            layout.getLineBottom(it) <= remaining
        }.size
        // Remeasure fragments: removing following lines can change final-line font metrics.
        while (lineCount > 0 &&
            measure(text.substring(0, layout.getLineEnd(lineCount - 1))).size.height > remaining
        ) {
            lineCount--
        }
        return if (lineCount == 0) 0 else layout.getLineEnd(lineCount - 1)
    }

    private fun add(block: CoverLetterPrintBlock, textHeight: Int) {
        pages.last().add(block)
        remaining -= textHeight + with(density) { block.gap.roundToPx() }
    }

    private fun newPage() {
        pages.add(mutableListOf())
        remaining = height
    }
}
