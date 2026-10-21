package reframecv.documents.coverletter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.chrisjenx.compose2pdf.InterFontFamily
import com.chrisjenx.compose2pdf.PdfLink

@Composable
internal fun CoverLetterPdfTemplate(
    blocks: List<CoverLetterPrintBlock>,
    pageIndex: Int,
    pageCount: Int,
) {
    CompositionLocalProvider(
        LocalContentColor provides Color.Black,
        LocalTextStyle provides CoverLetterPrintStyle.body,
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth()) {
                blocks.forEach { block -> PrintBlock(block) }
            }
            if (pageCount > 1) {
                Text(
                    "${pageIndex + 1} / $pageCount",
                    Modifier.align(Alignment.BottomCenter),
                    fontSize = 9.sp,
                    fontFamily = InterFontFamily,
                    color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun PrintBlock(block: CoverLetterPrintBlock) {
    val modifier = Modifier.fillMaxWidth().padding(bottom = block.gap)
    if (block.url == null) {
        Text(block.text, modifier)
    } else {
        PdfLink(block.url, modifier) {
            Text(
                block.text,
                color = CoverLetterPrintStyle.linkColor,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}
