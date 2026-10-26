package reframecv.ui.compose.common

import androidx.compose.ui.text.LinkAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectableLinkTextTest {
    private fun urls(text: String): List<String> = webLinks(text)
        .getLinkAnnotations(0, text.length).map { (it.item as LinkAnnotation.Url).url }

    @Test
    fun recognizesHttpAndHttpsWithoutChangingTheSourceText() {
        val text = "HTTP://example.test https://example.test/jobs?id=123&lang=en#details"
        assertEquals(text, webLinks(text).text)
        assertEquals(text.split(' '), urls(text))
    }

    @Test
    fun separatesLinksFromProseAndJsonPunctuationAndKeepsBalancedParentheses() {
        val text = """See (https://example.test/jobs/123), or {"site":"http://example.test"}."""
        assertEquals(listOf("https://example.test/jobs/123", "http://example.test"), urls(text))
        assertEquals(
            listOf("https://example.test/wiki/Thing_(Android)"),
            urls("See https://example.test/wiki/Thing_(Android)."),
        )
        val annotated = webLinks(text)
        annotated.getLinkAnnotations(0, text.length).forEach {
            assertEquals((it.item as LinkAnnotation.Url).url, text.substring(it.start, it.end))
        }
    }

    @Test
    fun leavesOtherSchemesAndMissingHostsAsOrdinaryText() {
        val text = "example.test mailto:hello@example.test file:///tmp/document " +
            "javascript:alert(1) https://"
        assertEquals(text, webLinks(text).text)
        assertTrue(urls(text).isEmpty())
    }
}
