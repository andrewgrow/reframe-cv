package reframecv.database.resume

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.SerializationException
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeConsent
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.sampleResumeContent

class ResumeContentJsonTest {
    @Test
    fun legacyConsentLoadsWithoutLosingTextOrVisibilityAndWritesOnlyNestedConsent() {
        val legacy = """{"formatVersion":1,"content":{
            "consentStatement":"Consent\nText","consentStatementVisible":false
        }}"""
        val content = ResumeContentJson.decode(legacy)
        assertEquals(ResumeConsent("Consent\nText", false), content.consent)
        val stored = ResumeEntity(
            projectId = 1,
            name = "Legacy resume",
            createdAt = 1,
            updatedAt = 1,
            contentJson = legacy,
        ).toDomainModel()
        assertEquals(content, stored.content)
        assertFalse(ResumeEntity.fromDomainModel(stored).contentJson.contains("consentStatement"))
        listOf(ResumeContentJson.encode(content), ResumeContentJson.encodeForExport(content))
            .forEach { encoded ->
                assertTrue(encoded.contains("\"consent\""))
                assertFalse(encoded.contains("consentStatement"))
                assertEquals(content, ResumeContentJson.decode(encoded))
            }
        val partial = """{"formatVersion":1,"content":{"consentStatement":"Text"}}"""
        assertEquals(ResumeConsent("Text"), ResumeContentJson.decode(partial).consent)
        val visibility = """{"formatVersion":1,"content":{"consentStatementVisible":false}}"""
        assertEquals(ResumeConsent(visible = false), ResumeContentJson.decode(visibility).consent)
    }

    @Test
    fun rejectsConflictingAndMalformedLegacyConsent() {
        assertFailsWith<IllegalArgumentException> {
            ResumeContentJson.decode(
                """{"formatVersion":1,"content":{"consent":{},"consentStatement":"Text"}}""",
            )
        }
        listOf(
            """{"formatVersion":1,"content":{"consentStatement":42}}""",
            """{"formatVersion":1,"content":{"consentStatementVisible":"yes"}}""",
        ).forEach { value ->
            assertFailsWith<SerializationException> { ResumeContentJson.decode(value) }
        }
    }

    @Test
    fun preservesEverySectionItsOrderVisibilityAndPartialDates() {
        val content = sampleResumeContent().copy(
            language = "DE",
            consent = ResumeConsent(text = "Consent\nText"),
        )
        val encoded = ResumeContentJson.encode(content)
        assertTrue(encoded.contains("\"formatVersion\":1"))
        assertEquals(content, ResumeContentJson.decode(encoded))
    }

    @Test
    fun emptyAndPartiallyFilledDocumentsRoundTrip() {
        val empty = ResumeContent()
        assertEquals(empty, ResumeContentJson.decode(ResumeContentJson.encode(empty)))
        val minimal = """{"formatVersion":1,"content":{"profile":{"name":"Alex"}}}"""
        assertEquals("Alex", ResumeContentJson.decode(minimal).profile.name)
        assertEquals(emptyList(), ResumeContentJson.decode(minimal).sections)
        assertEquals("", ResumeContentJson.decode(minimal).language)
        assertEquals("", ResumeContentJson.decode(minimal).consent.text)
        assertTrue(ResumeContentJson.decode(minimal).consent.visible)
        assertEquals(
            empty,
            ResumeEntity(
                projectId = 1,
                name = "Draft",
                createdAt = 1,
                updatedAt = 1,
            ).toDomainModel().content,
        )
    }

    @Test
    fun entityMappingPreservesContentAndMetadata() {
        val original = Resume(
            id = 3,
            projectId = 1,
            name = "Main",
            createdAt = 1,
            updatedAt = 2,
            content = sampleResumeContent(),
            sourceResumeId = 2,
        )
        assertEquals(original, ResumeEntity.fromDomainModel(original).toDomainModel())
    }

    @Test
    fun rejectsUnsupportedVersionsInsteadOfReadingOrOverwritingThem() {
        listOf(0, 2).forEach { version ->
            assertFailsWith<IllegalArgumentException> {
                ResumeContentJson.decode("""{"formatVersion":$version,"content":{}}""")
            }
        }
        assertFailsWith<IllegalArgumentException> {
            ResumeContentJson.decode("""{"content":{}}""")
        }
    }

    @Test
    fun rejectsMalformedUnknownAndInvalidContentInsteadOfReplacingItWithEmptyData() {
        assertFailsWith<SerializationException> { ResumeContentJson.decode("not json") }
        assertFailsWith<SerializationException> {
            ResumeContentJson.decode("""{"formatVersion":1,"content":{"unexpected":"data"}}""")
        }
        assertFailsWith<SerializationException> {
            ResumeContentJson.decode(
                """{"formatVersion":1,"content":{"sections":[{"type":"future"}]}}""",
            )
        }
        val invalid = ResumeContentJson.encode(
            sampleResumeContent(),
        ).replace("\"month\":9", "\"month\":13")
        assertFailsWith<IllegalArgumentException> { ResumeContentJson.decode(invalid) }
    }
}
