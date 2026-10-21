package reframecv.documents.resume

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.chrisjenx.compose2pdf.PdfLink

@Composable
internal fun ResumePdfTemplate(
    data: ResumePrintData,
    items: List<ResumePrintItem>,
    photo: ImageBitmap?,
    headerHeight: Dp,
    pageIndex: Int,
    pageCount: Int,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            if (pageIndex == 0 && headerHeight.value > 0) {
                ResumePdfHeader(data, photo, headerHeight)
            }
            items.forEach { ResumeText(it) }
        }
        if (pageCount > 1) {
            Text(
                "${pageIndex + 1} / $pageCount",
                Modifier.align(Alignment.BottomCenter),
                style = ResumePrintStyle.text(ResumePrintKind.Detail),
                color = Color.Gray,
            )
        }
    }
}

@Composable
internal fun ResumeText(item: ResumePrintItem) {
    val before = ResumePrintStyle.before(item)
    Column(Modifier.fillMaxWidth()) {
        if (before.value > 0) {
            if (item.ruleBefore) {
                Spacer(
                    Modifier.height(
                        before - ResumePrintStyle.ruleHeight - ResumePrintStyle.ruleGap,
                    ),
                )
                HorizontalDivider(
                    thickness = ResumePrintStyle.ruleHeight,
                    color = ResumePrintStyle.accent,
                )
                Spacer(Modifier.height(ResumePrintStyle.ruleGap))
            } else {
                Spacer(Modifier.height(before))
            }
        }
        val modifier = Modifier.fillMaxWidth().padding(bottom = ResumePrintStyle.gap(item))
        val style = ResumePrintStyle.text(item.kind)
        val text = styledResumeText(item)
        when {
            item.kind == ResumePrintKind.Bullet -> Row(modifier) {
                if (item.continuation) {
                    Spacer(Modifier.width(ResumePrintStyle.bulletIndent))
                } else {
                    Text("•", Modifier.width(ResumePrintStyle.bulletIndent), style = style)
                }
                Text(text, Modifier.weight(1f), style = style)
            }

            item.url == null -> Text(text, modifier, style = style)

            else -> PdfLink(item.url, modifier) {
                Text(
                    text,
                    style = style,
                    color = ResumePrintStyle.linkColor,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}
