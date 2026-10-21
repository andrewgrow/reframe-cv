package reframecv.documents.resume

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

internal fun styledResumeText(item: ResumePrintItem): AnnotatedString = buildAnnotatedString {
    append(item.text)
    if (item.boldPrefixLength > 0) {
        addStyle(
            SpanStyle(fontWeight = FontWeight.Bold),
            0,
            item.boldPrefixLength.coerceAtMost(length),
        )
    }
    item.italicSuffixStart?.takeIf { it < length }?.let {
        addStyle(
            SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal),
            it.coerceAtLeast(0),
            length,
        )
    }
}

internal fun ResumePrintItem.slice(start: Int, end: Int = text.length) = copy(
    text = text.substring(start, end),
    boldPrefixLength = (boldPrefixLength - start).coerceIn(0, end - start),
    italicSuffixStart = italicSuffixStart?.minus(start)?.coerceAtLeast(0),
    continuation = continuation || start > 0,
    ruleBefore = ruleBefore && start == 0,
    paragraphEnd = paragraphEnd && end == text.length,
)
