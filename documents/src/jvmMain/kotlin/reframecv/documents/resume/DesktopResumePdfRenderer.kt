package reframecv.documents.resume

import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.chrisjenx.compose2pdf.PdfMargins
import com.chrisjenx.compose2pdf.PdfPageConfig
import com.chrisjenx.compose2pdf.RenderMode
import com.chrisjenx.compose2pdf.renderToPdf
import org.jetbrains.skia.Image
import reframecv.documents.PdfRenderLock

/** Renders measured resume pages, including a portrait only in the first-page header. */
object DesktopResumePdfRenderer {
    private val page = PdfPageConfig.A4.copy(
        margins = PdfMargins(left = 85.dp, right = 42.dp, top = 56.7.dp, bottom = 56.7.dp),
    )

    fun render(data: ResumePrintData): ByteArray = synchronized(PdfRenderLock) {
        val photo = data.photo?.let { Image.makeFromEncoded(it).toComposeImageBitmap() }
        val paginator = ResumePaginator(page, data)
        val pages = paginator.paginate()
        val headerHeight = with(ResumePrintStyle.density) { paginator.headerHeight.toDp() }
        renderToPdf(
            pages = pages.size,
            config = page,
            density = ResumePrintStyle.density,
            mode = RenderMode.VECTOR,
            defaultFontFamily = ResumeSerif,
        ) { index ->
            ResumePdfTemplate(data, pages[index], photo, headerHeight, index, pages.size)
        }
    }
}
