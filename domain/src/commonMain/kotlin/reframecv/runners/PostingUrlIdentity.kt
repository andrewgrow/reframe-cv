package reframecv.runners

/** Identifies a posting within its originating website, never by a scraper-local numeric ID alone. */
data class SourcePostingIdentity(val source: String, val externalId: String)

/** Retains a conservative canonical address and an optional website-specific posting identity. */
data class PostingUrlIdentity(val canonicalUrl: String, val sourceIdentity: SourcePostingIdentity?)

/** Normalizes transport/tracking details while preserving path case and identifying query values. */
fun postingUrlIdentity(value: String): PostingUrlIdentity? =
    POSTING_URL.matchEntire(value.trim())?.let { parts ->
        val scheme = parts.groupValues[1].lowercase()
        val authority = parts.groupValues[2].lowercase()
        val host = authority.removeSuffix(if (scheme == "https") ":443" else ":80")
        val unsafe = '@' in authority || '\\' in value || host.isBlank()
        if (unsafe) {
            null
        } else {
            val path = parts.groupValues[3].trimEnd('/')
            val query = canonicalQuery(parts.groupValues[4])
            val canonical = "$scheme://$host$path" + if (query.isEmpty()) "" else "?$query"
            PostingUrlIdentity(canonical, sourcePostingIdentity(host, path))
        }
    }

private fun canonicalQuery(query: String): String = query.split('&')
    .filter { it.isNotEmpty() && !trackingParameter(it.substringBefore('=')) }
    .sortedBy { it.substringBefore('=') }
    .joinToString("&")

private fun trackingParameter(key: String): Boolean {
    val normalized = key.lowercase()
    return normalized.startsWith("utm_") || normalized in setOf("gclid", "fbclid", "msclkid")
}

private fun sourcePostingIdentity(host: String, path: String): SourcePostingIdentity? = when {
    host == "linkedin.com" || host.endsWith(".linkedin.com") -> {
        val posting = LINKEDIN_POSTING.matchEntire(path)?.groupValues?.get(1)
        val id = posting?.substringAfterLast('-')?.takeIf { it.matches(POSTING_ID) }
        id?.let { SourcePostingIdentity("LinkedIn", it) }
    }

    host == "ams.at" || host.endsWith(".ams.at") -> {
        AMS_POSTING.matchEntire(path)?.groupValues?.get(1)?.let {
            SourcePostingIdentity("AMS", it)
        }
    }

    else -> null
}

private val POSTING_URL = Regex(
    "^(https?)://([^/?#\\s]+)([^?#\\s]*)(?:\\?([^#\\s]*))?(?:#[^\\s]*)?$",
    RegexOption.IGNORE_CASE,
)
private val LINKEDIN_POSTING = Regex("/jobs/view/([^/]+)", RegexOption.IGNORE_CASE)
private val AMS_POSTING = Regex("/(?:public/emps/jobs|job)/([0-9]+)", RegexOption.IGNORE_CASE)
private val POSTING_ID = Regex("[0-9]+")
