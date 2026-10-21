package reframecv.documents.resume

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.chrisjenx.compose2pdf.PdfLink

@Composable
internal fun ResumePdfHeader(data: ResumePrintData, photo: ImageBitmap?, height: Dp) {
    Row(Modifier.fillMaxWidth().height(height)) {
        Column(Modifier.weight(1f)) {
            data.header.filter { it.kind == ResumePrintKind.Name }.forEach { ResumeText(it) }
            FlowRow {
                data.contacts.forEachIndexed { index, contact ->
                    val label = contactLabel(data, index)
                    if (contact.url.isEmpty()) {
                        Text(label, style = contactStyle())
                    } else {
                        PdfLink(contact.url) {
                            Text(
                                label,
                                style = contactStyle(),
                                color = ResumePrintStyle.linkColor,
                                textDecoration = TextDecoration.Underline,
                            )
                        }
                    }
                }
            }
            data.header.filter { it.kind != ResumePrintKind.Name }.forEach { ResumeText(it) }
        }
        if (photo != null) {
            Spacer(Modifier.width(ResumePrintStyle.headerGap))
            Image(
                photo,
                null,
                Modifier.size(ResumePrintStyle.photoWidth, ResumePrintStyle.photoHeight),
            )
        }
    }
}
