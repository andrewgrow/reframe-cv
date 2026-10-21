package reframecv.documents.coverletter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chrisjenx.compose2pdf.InterFontFamily

/** Keeps measurement and rendering on the same fixed print typography. */
internal object CoverLetterPrintStyle {
    val density = Density(2f)
    val body = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = Color.Black,
        // Avoid contextual glyph substitutions with ambiguous SVG-to-Unicode mappings.
        fontFeatureSettings = "liga 0, calt 0, dlig 0",
    )
    val footerHeight = 20.dp
    val paragraphGap = 12.dp
    val linkGap = 8.dp
    val linkColor = Color(0xFF2454A6)
}
