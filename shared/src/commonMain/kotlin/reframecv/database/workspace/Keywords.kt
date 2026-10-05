package reframecv.database.workspace

internal fun normalizeKeywords(keywords: List<String>): List<String> = keywords
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinctBy(String::lowercase)
