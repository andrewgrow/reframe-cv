package reframecv.providers.apify

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import reframecv.domain.models.providers.RunnerResult

class ApifyResultConverterTest {
    private fun row(json: String, source: String = "LinkedIn") =
        RunnerResult(1, 1, 0, source, "101", json, 100)

    @Test
    fun mapsKnownSourceLocallyWithoutNetworkAndUsesImportTimeAsCreationTime() {
        val source =
            row(
                """{"id":"101","title":"Mobile engineer","companyName":"ExampleCo",
            "descriptionText":"Build &amp; maintain apps","postedAt":"2020-01-01","unknown":{"x":1}}""",
            )
        val vacancy = ApifyResultConverter().vacancy(source, 7, 999)
        assertEquals("Mobile engineer", vacancy.name)
        assertEquals("Mobile engineer", vacancy.jobTitle)
        assertEquals("ExampleCo", vacancy.company)
        assertEquals("Build & maintain apps", vacancy.description)
        assertEquals("https://www.linkedin.com/jobs/view/101", vacancy.url)
        assertEquals(7, vacancy.projectId)
        assertEquals(999, vacancy.createdAt)
    }

    @Test
    fun invalidAndUnsupportedSnapshotsAreNotSilentlyTurnedIntoVacancies() {
        assertFailsWith<IllegalStateException> { ApifyResultConverter().vacancy(row("{}"), 1, 1) }
        assertFailsWith<IllegalArgumentException> {
            ApifyResultConverter().vacancy(row("not json"), 1, 1)
        }
        assertFailsWith<IllegalArgumentException> {
            ApifyResultConverter().vacancy(row("{}", "Other"), 1, 1)
        }
    }
}
