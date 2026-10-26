package reframecv.documents.vacancy

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.chrisjenx.compose2pdf.PdfPageConfig

/** Splits measured Compose text before rendering, so off-page text never enters the PDF. */
internal class VacancyPaginator(config: PdfPageConfig) {
    private val density = VacancyPrintStyle.density
    private val width = with(density) { config.contentWidth.roundToPx() }
    private val height = with(density) {
        (config.contentHeight - VacancyPrintStyle.footerHeight).roundToPx()
    }
    private val measurer = TextMeasurer(
        createFontFamilyResolver(),
        density,
        LayoutDirection.Ltr,
    )
    private val pages = mutableListOf<MutableList<VacancyPrintBlock>>(mutableListOf())
    private var remaining = height

    fun paginate(data: VacancyPrintData): List<List<VacancyPrintBlock>> {
        val blocks = data.blocks()
        blocks.forEachIndexed { index, block ->
            if (block.keepWithNext && index < blocks.lastIndex) {
                val next = blocks[index + 1]
                val needed = measure(block).size.height + measure(next).getLineBottom(0) +
                    with(density) { VacancyPrintStyle.gap(block).roundToPx() }
                if (needed > remaining && remaining < height) newPage()
            }
            append(block)
        }
        return pages
    }

    private fun measure(block: VacancyPrintBlock) = measurer.measure(
        block.text,
        VacancyPrintStyle.text(block.kind),
        constraints = Constraints(
            maxWidth = width - if (block.kind == VacancyPrintKind.Bullet) {
                with(density) { VacancyPrintStyle.bulletIndent.roundToPx() }
            } else {
                0
            },
        ),
    )

    private fun append(block: VacancyPrintBlock) {
        var rest = block
        while (rest.text.isNotEmpty()) {
            val layout = measure(rest)
            if (layout.size.height <= height && layout.size.height > remaining &&
                pages.last().lastOrNull()?.keepWithNext != true
            ) {
                newPage()
            }
            if (layout.size.height <= remaining) {
                add(rest, layout.size.height)
                rest = rest.copy(text = "")
            } else {
                val end = fittingEnd(rest)
                if (end == 0) {
                    check(pages.last().isNotEmpty()) { "A text line exceeds the printable height" }
                    newPage()
                } else {
                    val fragment = rest.copy(
                        text = rest.text.substring(0, end),
                        paragraphEnd = false,
                    )
                    add(fragment, measure(fragment).size.height)
                    rest = rest.copy(text = rest.text.substring(end), continuation = true)
                    newPage()
                }
            }
        }
    }

    private fun fittingEnd(block: VacancyPrintBlock): Int {
        val text = block.text
        val layout = measure(block)
        var lineCount = (0 until layout.lineCount).takeWhile {
            layout.getLineBottom(it) <= remaining
        }.size
        // Remeasure fragments: removing following lines can change final-line font metrics.
        while (lineCount > 0 &&
            measure(
                block.copy(text = text.substring(0, layout.getLineEnd(lineCount - 1))),
            ).size.height >
            remaining
        ) {
            lineCount--
        }
        return if (lineCount == 0) 0 else layout.getLineEnd(lineCount - 1)
    }

    private fun add(block: VacancyPrintBlock, textHeight: Int) {
        pages.last().add(block)
        remaining -= textHeight + with(density) { VacancyPrintStyle.gap(block).roundToPx() }
    }

    private fun newPage() {
        pages.add(mutableListOf())
        remaining = height
    }
}
