package reframecv.ui.compose.common

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

/** Makes web addresses actionable while retaining the original selectable source text. */
@Composable
internal fun SelectableLinkText(
    text: String,
    modifier: Modifier = Modifier,
    openUrl: (String) -> Unit = LocalUriHandler.current::openUri,
) {
    val color = MaterialTheme.colorScheme.primary
    val annotated = remember(text, color, openUrl) {
        webLinks(
            text,
            TextLinkStyles(SpanStyle(color, textDecoration = TextDecoration.Underline)),
            openUrl,
        )
    }
    SelectionContainer { Text(annotated, modifier) }
}

internal fun webLinks(
    text: String,
    styles: TextLinkStyles = TextLinkStyles(),
    openUrl: ((String) -> Unit)? = null,
): AnnotatedString = buildAnnotatedString {
    append(text)
    WEB_ADDRESS.findAll(text).forEach { match ->
        val url = trimWebAddress(match.value)
        val authority = url.substringAfter("://").substringBefore('/')
            .substringBefore('?').substringBefore('#')
        if (authority.isNotEmpty()) {
            addLink(
                LinkAnnotation.Url(
                    url,
                    styles,
                    openUrl?.let { open -> LinkInteractionListener { open(url) } },
                ),
                match.range.first,
                match.range.first + url.length,
            )
        }
    }
}

private fun trimWebAddress(value: String): String {
    var url = value.trimEnd('.', ',', ';', '!')
    val brackets = mapOf(')' to '(', ']' to '[', '}' to '{')
    while (url.lastOrNull() in brackets) {
        val end = url.last()
        val start = brackets.getValue(end)
        if (url.count { it == end } <= url.count { it == start }) break
        url = url.dropLast(1).trimEnd('.', ',', ';', '!')
    }
    return url
}

private val WEB_ADDRESS = Regex("""\bhttps?://[^\s<>"'\\]+""", RegexOption.IGNORE_CASE)
