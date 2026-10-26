package reframecv.documents.vacancy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.chrisjenx.compose2pdf.InterFontFamily
import com.chrisjenx.compose2pdf.PdfLink

@Composable
internal fun VacancyPdfTemplate(blocks: List<VacancyPrintBlock>, index: Int, count: Int) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) { blocks.forEach { VacancyText(it) } }
        if (count > 1) {
            Text(
                "${index + 1} / $count",
                Modifier.align(Alignment.BottomCenter),
                color = Color.Gray,
                fontFamily = InterFontFamily,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun VacancyText(block: VacancyPrintBlock) {
    val modifier = Modifier.fillMaxWidth().padding(bottom = VacancyPrintStyle.gap(block))
    val style = VacancyPrintStyle.text(block.kind)
    when {
        block.kind == VacancyPrintKind.Bullet -> Row(modifier) {
            if (block.continuation) {
                Spacer(Modifier.width(VacancyPrintStyle.bulletIndent))
            } else {
                Text("•", Modifier.width(VacancyPrintStyle.bulletIndent), style = style)
            }
            Text(block.text, Modifier.weight(1f), style = style)
        }

        block.url != null -> PdfLink(block.url, modifier) {
            Text(
                block.text,
                style = style,
                color = VacancyPrintStyle.linkColor,
                textDecoration = TextDecoration.Underline,
            )
        }

        else -> Text(block.text, modifier, style = style)
    }
}
