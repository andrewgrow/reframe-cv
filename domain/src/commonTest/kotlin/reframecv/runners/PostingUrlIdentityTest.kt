package reframecv.runners

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PostingUrlIdentityTest {
    @Test
    fun canonicalAddressesRemoveTrackingAndFragmentsButKeepJobQueryParametersAndPathCase() {
        assertEquals(
            "https://careers.example.test/Jobs?jobId=101&lang=en",
            postingUrlIdentity(
                " HTTPS://CAREERS.EXAMPLE.TEST:443/Jobs/?utm_source=board&lang=en&jobId=101#details ",
            )?.canonicalUrl,
        )
        assertNotEquals(
            postingUrlIdentity("https://careers.example.test/Jobs?jobId=101"),
            postingUrlIdentity("https://careers.example.test/Jobs?jobId=102"),
        )
        assertNotEquals(
            postingUrlIdentity("https://careers.example.test/Jobs?jobId=101"),
            postingUrlIdentity("https://careers.example.test/jobs?jobId=101"),
        )
        assertEquals(
            "https://careers.example.test/job?ref=101&tags=a&tags=b",
            postingUrlIdentity(
                "https://careers.example.test/job?tags=a&ref=101&tags=b&fbclid=tracking",
            )?.canonicalUrl,
        )
    }

    @Test
    fun websiteAdaptersRecoverIdentityAndNeverConfuseIdsFromDifferentWebsites() {
        val professional =
            postingUrlIdentity("https://at.linkedin.com/jobs/view/example-role-101?trk=x")
        val publicBoard = postingUrlIdentity("https://jobs.ams.at/public/emps/jobs/101?tracking=x")
        assertEquals(SourcePostingIdentity("LinkedIn", "101"), professional?.sourceIdentity)
        assertEquals(SourcePostingIdentity("AMS", "101"), publicBoard?.sourceIdentity)
        assertNotEquals(professional?.sourceIdentity, publicBoard?.sourceIdentity)
        assertNull(postingUrlIdentity("https://example.test/jobs/view/101")?.sourceIdentity)
        assertNull(
            postingUrlIdentity("https://linkedin.com.evil.test/jobs/view/101")?.sourceIdentity,
        )
        assertNull(
            postingUrlIdentity("https://jobs.ams.at/public/emps/jobs/101/other")?.sourceIdentity,
        )
    }

    @Test
    fun invalidAddressesAreIgnored() {
        listOf(
            "",
            "not a URL",
            "file:///tmp/a",
            "javascript:alert(1)",
            "https://example.test/a b",
            "https://user@example.test/job",
            "https://example.test\\job",
        )
            .forEach { assertNull(postingUrlIdentity(it), it) }
    }
}
