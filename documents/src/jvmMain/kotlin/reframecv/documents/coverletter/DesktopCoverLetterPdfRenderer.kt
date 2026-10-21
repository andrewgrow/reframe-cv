package reframecv.documents.coverletter

import androidx.compose.ui.unit.dp
import com.chrisjenx.compose2pdf.PdfMargins
import com.chrisjenx.compose2pdf.PdfPageConfig
import com.chrisjenx.compose2pdf.RenderMode
import com.chrisjenx.compose2pdf.renderToPdf

private const val PAGE_MARGIN = 56.7f

/** Renders separately paginated Compose pages with searchable text and working links. */
object DesktopCoverLetterPdfRenderer {
    private val lock = Any()
    private val page = PdfPageConfig.A4.copy(
        margins = PdfMargins.symmetric(horizontal = PAGE_MARGIN.dp, vertical = PAGE_MARGIN.dp),
    )

    fun render(data: CoverLetterPrintData): ByteArray = synchronized(lock) {
        require(data.text.isNotBlank()) { "Cover letter text must not be blank" }
        require(data.links.all { it.label.isNotBlank() && it.url.isNotBlank() })
        val pages = CoverLetterPaginator(page).paginate(data)
        renderToPdf(
            pages = pages.size,
            config = page,
            density = CoverLetterPrintStyle.density,
            mode = RenderMode.VECTOR,
        ) { index -> CoverLetterPdfTemplate(pages[index], index, pages.size) }
    }
}
