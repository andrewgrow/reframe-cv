package reframecv.documents.resume

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.chrisjenx.compose2pdf.PdfPageConfig

/** Splits measured Compose text before rendering, so off-page text never enters the PDF. */
internal class ResumePaginator(config: PdfPageConfig, private val data: ResumePrintData) {
    private val density = ResumePrintStyle.density
    private val width = with(density) { config.contentWidth.roundToPx() }
    private val height = with(density) {
        (config.contentHeight - ResumePrintStyle.footerHeight).roundToPx()
    }
    private val measurer = TextMeasurer(
        createFontFamilyResolver(),
        density,
        LayoutDirection.Ltr,
    )
    private val pages = mutableListOf<MutableList<ResumePrintItem>>(mutableListOf())
    val headerHeight = measureHeader()
    private var remaining = height - headerHeight

    private fun measureHeader(): Int {
        val photoWidth = if (data.photo == null) {
            0
        } else {
            with(density) {
                (ResumePrintStyle.photoWidth + ResumePrintStyle.headerGap).roundToPx()
            }
        }
        val textHeight =
            contactStripHeight(data, width - photoWidth, measurer) + data.header.sumOf {
                measure(it, width - photoWidth).size.height + with(density) {
                    (ResumePrintStyle.before(it) + ResumePrintStyle.gap(it)).roundToPx()
                }
            }
        val portraitHeight = if (data.photo == null) {
            0
        } else {
            with(density) {
                ResumePrintStyle.photoHeight.roundToPx()
            }
        }
        val contentHeight = maxOf(textHeight, portraitHeight)
        require(contentHeight < height) { "Resume profile exceeds a page" }
        return contentHeight
    }

    fun paginate(): List<List<ResumePrintItem>> {
        val items = data.items.filter { it.text.isNotBlank() }
        items.forEachIndexed { index, item ->
            val needed = minimumGroupHeight(items, index)
            if (item.keepWithNext && needed > remaining && remaining < height) newPage()
            append(item)
        }
        return pages
    }

    private fun minimumGroupHeight(items: List<ResumePrintItem>, index: Int): Int {
        var needed = 0
        var position = index
        do {
            val item = items[position]
            val layout = measure(item)
            needed += if (item.keepWithNext) {
                layout.size.height + with(density) {
                    (ResumePrintStyle.before(item) + ResumePrintStyle.gap(item)).roundToPx()
                }
            } else {
                layout.getLineBottom(0).toInt() +
                    with(density) { ResumePrintStyle.before(item).roundToPx() }
            }
            position++
        } while (item.keepWithNext && position < items.size)
        return needed
    }

    private fun measure(item: ResumePrintItem, maxWidth: Int = width) = measurer.measure(
        styledResumeText(item),
        ResumePrintStyle.text(item.kind),
        constraints = Constraints(
            maxWidth = maxWidth - if (item.kind == ResumePrintKind.Bullet) {
                with(density) { ResumePrintStyle.bulletIndent.roundToPx() }
            } else {
                0
            },
        ),
    )

    private fun append(block: ResumePrintItem) {
        var rest = block
        while (rest.text.isNotEmpty()) {
            val layout = measure(rest)
            val occupied =
                layout.size.height + with(density) { ResumePrintStyle.before(rest).roundToPx() }
            if (occupied <= height && occupied > remaining) newPage()
            if (occupied <= remaining) {
                add(rest, layout.size.height)
                rest = rest.copy(text = "")
            } else {
                val end = fittingEnd(rest)
                if (end == 0) {
                    check(remaining < height) { "A text line exceeds the printable height" }
                    newPage()
                } else {
                    val fragment = rest.slice(0, end)
                    add(fragment, measure(fragment).size.height)
                    rest = rest.slice(end)
                    newPage()
                }
            }
        }
    }

    private fun fittingEnd(item: ResumePrintItem): Int {
        val text = item.text
        val layout = measure(item)
        val available = remaining - with(density) { ResumePrintStyle.before(item).roundToPx() }
        var lineCount = (0 until layout.lineCount).takeWhile {
            layout.getLineBottom(it) <= available
        }.size
        // Remeasure fragments: removing following lines can change final-line font metrics.
        while (lineCount > 0 &&
            measure(
                item.slice(0, layout.getLineEnd(lineCount - 1)),
            ).size.height >
            available
        ) {
            lineCount--
        }
        return if (lineCount == 0) 0 else layout.getLineEnd(lineCount - 1)
    }

    private fun add(block: ResumePrintItem, textHeight: Int) {
        pages.last().add(block)
        remaining -= textHeight + with(density) {
            (ResumePrintStyle.before(block) + ResumePrintStyle.gap(block)).roundToPx()
        }
    }

    private fun newPage() {
        pages.add(mutableListOf())
        remaining = height
    }
}
