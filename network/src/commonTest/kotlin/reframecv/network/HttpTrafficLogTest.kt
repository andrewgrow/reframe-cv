package reframecv.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class HttpTrafficLogTest {
    @Test
    fun retainsUnusedResponseFieldsWhileRedactingHeadersAndNestedSecrets() = runBlocking<Unit> {
        val messages = mutableListOf<String>()
        val logger = HttpTrafficLog(HttpTrafficLogger { messages += it })
        logger.request(
            "GET",
            "https://openrouter.ai/api/v1/key",
            mapOf("Authorization" to "Bearer example-secret"),
            listOf("example-secret"),
        )
        logger.response(
            "GET",
            "https://openrouter.ai/api/v1/key",
            200,
            mapOf("Content-Type" to "application/json", "Set-Cookie" to "session=private"),
            """
            {"data":{"limit_remaining":123.45,"label":"Test key","api_key":"another-secret",
            "nested":[{"access_token":"hidden"}]},"unknown":{"new_field":true},"echo":"example-secret"}
            """.trimIndent(),
            listOf("example-secret"),
        )
        val text = messages.joinToString("\n")
        assertTrue(text.contains("REQUEST GET"))
        assertTrue(text.contains("RESPONSE 200 GET"))
        assertTrue(text.contains("limit_remaining"))
        assertTrue(text.contains("123.45"))
        assertTrue(text.contains("new_field"))
        assertTrue(text.contains("[REDACTED]"))
        listOf("example-secret", "another-secret", "hidden", "session=private").forEach {
            assertFalse(text.contains(it))
        }
    }

    @Test
    fun recordsPlainTextErrorsWithoutEchoingTheKeyOrExceptionMessage() = runBlocking<Unit> {
        val messages = mutableListOf<String>()
        val logger = HttpTrafficLog(HttpTrafficLogger { messages += it })
        logger.response(
            "GET",
            "https://openrouter.ai/api/v1/key",
            401,
            emptyMap(),
            "Rejected example-secret",
            listOf("example-secret"),
        )
        logger.failure(
            "GET",
            "https://openrouter.ai/api/v1/key",
            IllegalStateException("example-secret"),
        )
        assertTrue(messages.first().contains("RESPONSE 401"))
        assertTrue(messages.last().contains("IllegalStateException"))
        assertFalse(messages.joinToString().contains("example-secret"))
    }

    @Test
    fun loggingFailuresDoNotBreakApiOperations() = runBlocking<Unit> {
        var writes = 0
        val logger = HttpTrafficLog(
            HttpTrafficLogger {
                writes++
                error("disk unavailable")
            },
        )
        logger.request("GET", "https://openrouter.ai/api/v1/key", emptyMap(), emptyList())
        assertEquals(1, writes)
    }
}
