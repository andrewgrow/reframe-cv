package reframecv.documents.resume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Uses identical dimensions and type metrics for measurement and rendering. */
internal object ResumePrintStyle {
    val density = Density(2f)
    val photoWidth = 99.2.dp
    val photoHeight = 127.6.dp
    val headerGap = 14.dp
    val footerHeight = 20.dp
    val linkColor = Color(0xFF0000EE)
    val accent = Color(0xFF002060)
    val ruleHeight = 0.5.dp
    val ruleGap = 10.dp
    private val body = TextStyle(
        fontFamily = ResumeSerif,
        fontSize = 10.5.sp,
        lineHeight = 12.2.sp,
        color = Color.Black,
        fontFeatureSettings = "liga 0, calt 0, dlig 0",
    )

    fun text(kind: ResumePrintKind): TextStyle = when (kind) {
        ResumePrintKind.Name -> body.copy(
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )

        ResumePrintKind.Headline, ResumePrintKind.Section -> body.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )

        ResumePrintKind.Subheading, ResumePrintKind.Role, ResumePrintKind.SummaryLead ->
            body.copy(fontWeight = FontWeight.Bold)

        ResumePrintKind.Detail, ResumePrintKind.Body, ResumePrintKind.Bullet,
        ResumePrintKind.Link, ResumePrintKind.Consent,
        -> body
    }

    val bulletIndent = 18.dp

    fun before(item: ResumePrintItem) = if (item.continuation) {
        0.dp
    } else {
        when {
            item.ruleBefore -> 12.dp + ruleHeight + ruleGap
            item.kind == ResumePrintKind.Section -> 12.dp
            item.kind == ResumePrintKind.Headline -> 8.dp
            item.kind == ResumePrintKind.Link -> 6.dp
            item.kind == ResumePrintKind.Consent -> 24.dp
            else -> 0.dp
        }
    }

    fun gap(item: ResumePrintItem) = if (!item.paragraphEnd) {
        0.dp
    } else {
        when (item.kind) {
            ResumePrintKind.Name -> 8.dp
            ResumePrintKind.Headline -> 12.dp
            ResumePrintKind.Section -> 10.dp
            ResumePrintKind.Role -> 10.dp
            ResumePrintKind.SummaryLead -> 8.dp
            else -> 2.dp
        }
    }
}
