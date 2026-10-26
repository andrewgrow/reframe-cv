package reframecv.documents.vacancy

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chrisjenx.compose2pdf.InterFontFamily

/** Keeps measured text and rendered vacancy pages on the same print typography. */
internal object VacancyPrintStyle {
    val density = Density(2f)
    val footerHeight = 20.dp
    val bulletIndent = 14.dp
    val linkColor = Color(0xFF2454A6)
    private val body = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = Color.Black,
        fontFeatureSettings = "liga 0, calt 0, dlig 0",
    )
    fun text(kind: VacancyPrintKind): TextStyle = when (kind) {
        VacancyPrintKind.Title -> body.copy(
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
        )

        VacancyPrintKind.Company -> body.copy(fontSize = 13.sp, lineHeight = 19.sp)

        VacancyPrintKind.Section -> body.copy(
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        else -> body
    }
    fun gap(block: VacancyPrintBlock) = when {
        !block.paragraphEnd -> 0.dp
        block.kind == VacancyPrintKind.Section -> 6.dp
        block.kind == VacancyPrintKind.Bullet -> 6.dp
        else -> 12.dp
    }
}
