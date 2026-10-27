package reframecv.providers.apify

internal fun htmlText(value: String) = value
    .replace(Regex("<(script|style)\\b[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("<br\\s*/?>|</(?:p|div|ul|ol|li|h[1-6])>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<li\\b[^>]*>", RegexOption.IGNORE_CASE), "- ")
    .replace(Regex("<[^>]*>"), "")

internal fun cleanText(value: String): String {
    val entities = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"",
        "apos" to "'", "nbsp" to " ", "#39" to "'", "#34" to "\"", "#160" to " ",
    )
    return value.replace(Regex("&(amp|lt|gt|quot|apos|nbsp|#[0-9]+|#[xX][0-9a-fA-F]+);")) {
        val entity = it.groupValues[1]
        entities[entity] ?: numericEntity(entity) ?: it.value
    }.replace("\r\n", "\n").replace("\r", "\n")
        .lines().joinToString("\n") { it.trim() }.replace(Regex("\n{3,}"), "\n\n").trim()
}

@Suppress("MagicNumber") // Unicode scalar ranges and UTF-16 surrogate encoding.
private fun numericEntity(value: String): String? {
    val code = if (value.startsWith("#x", true)) {
        value.drop(2).toIntOrNull(16)
    } else {
        value.drop(1).toIntOrNull()
    } ?: return null
    return when (code) {
        in 1..0xD7FF, in 0xE000..0xFFFF -> code.toChar().toString()

        in 0x10000..0x10FFFF -> {
            val offset = code - 0x10000
            "${(0xD800 + (offset shr 10)).toChar()}${(0xDC00 + (offset and 0x3FF)).toChar()}"
        }

        else -> null
    }
}
