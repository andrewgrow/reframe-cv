package reframecv.documents.resume

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp

internal fun contactLabel(data: ResumePrintData, index: Int): String =
    data.contacts[index].label + if (index < data.contacts.lastIndex) " | " else ""

/** Mirrors the contact strip's FlowRow wrapping during page measurement. */
internal fun contactStripHeight(data: ResumePrintData, width: Int, measurer: TextMeasurer): Int {
    var used = 0
    var height = 0
    var rowHeight = 0
    data.contacts.indices.forEach { index ->
        val layout = measurer.measure(
            contactLabel(data, index),
            style = contactStyle(),
            constraints = Constraints(maxWidth = width),
        )
        if (used > 0 && used + layout.size.width > width) {
            height += rowHeight
            used = 0
            rowHeight = 0
        }
        used += layout.size.width
        rowHeight = maxOf(rowHeight, layout.size.height)
    }
    return height + rowHeight
}

internal fun contactStyle(): TextStyle = ResumePrintStyle.text(ResumePrintKind.Detail).copy(
    fontSize = 12.sp,
    lineHeight = 14.sp,
    color = ResumePrintStyle.accent,
)
