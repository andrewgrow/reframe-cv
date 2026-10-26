package reframecv.documents.vacancy

import androidx.compose.ui.unit.dp
import com.chrisjenx.compose2pdf.PdfMargins
import com.chrisjenx.compose2pdf.PdfPageConfig
import com.chrisjenx.compose2pdf.RenderMode
import com.chrisjenx.compose2pdf.renderToPdf
import reframecv.documents.PdfRenderLock

/** Renders paginated A4 vacancies with selectable text and a clickable source link. */
object DesktopVacancyPdfRenderer {
    private val page = PdfPageConfig.A4.copy(
        margins = PdfMargins.symmetric(horizontal = 56.7.dp, vertical = 56.7.dp),
    )
    fun render(data: VacancyPrintData): ByteArray = synchronized(PdfRenderLock) {
        require(data.title.isNotBlank()) { "Vacancy title must not be blank" }
        val pages = VacancyPaginator(page).paginate(data)
        renderToPdf(
            pages = pages.size,
            config = page,
            density = VacancyPrintStyle.density,
            mode = RenderMode.VECTOR,
        ) { index -> VacancyPdfTemplate(pages[index], index, pages.size) }
    }
}
