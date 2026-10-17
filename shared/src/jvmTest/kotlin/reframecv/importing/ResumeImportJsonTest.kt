package reframecv.importing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.serialization.SerializationException
import reframecv.domain.models.resume.content.ResumeConsent
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeDate
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeSkillsSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.serialization.ResumeContentJson

class ResumeImportJsonTest {
    @Test
    fun exportedDocumentImportsEverySectionInOrderWithDatesAndHiddenConsent() {
        val content = sampleResumeContent().copy(
            language = "DE",
            consent = ResumeConsent("Consent\nText", false),
        )
        assertEquals(content, ResumeImportJson.decode(ResumeContentJson.encodeForExport(content)))
        assertEquals(content, ResumeImportJson.decode("\uFEFF" + ResumeContentJson.encode(content)))
    }

    @Test
    fun skipsUnknownFieldsSectionsWrongTypesAndInvalidItemsWhileKeepingValidSiblings() {
        val document = """{"formatVersion":1,"unknown":true,"content":{
            "language":"EN","unknown":true,
            "profile":{"name":"Alex","headline":42,"location":"Berlin","extra":true},
            "consent":{"text":"Consent","visible":"yes","future":true},
            "sections":[
                {"type":"future","text":"Skip"},42,
                {"type":"summary","text":"Keep","visible":false,"future":true},
                {"type":"skills","groups":[null,42,{"title":"Mobile",
                    "items":["Kotlin",42,null,"Compose"],"unknown":true}]}
            ]
        }}"""
        val content = ResumeImportJson.decode(document)
        assertEquals("Alex", content.profile.name)
        assertEquals("", content.profile.headline)
        assertEquals("Berlin", content.profile.location)
        assertEquals("EN", content.language)
        assertEquals(ResumeConsent("Consent"), content.consent)
        assertEquals(2, content.sections.size)
        assertEquals("Keep", assertIs<ResumeSummarySection>(content.sections[0]).text)
        assertEquals(false, content.sections[0].visible)
        val groups = assertIs<ResumeSkillsSection>(content.sections[1]).groups
        assertEquals(listOf("Kotlin", "Compose"), groups.single().items)
    }

    @Test
    fun recoversInvalidDatesWithoutDiscardingEducationOrExperienceFields() {
        val document = """{"formatVersion":1,"content":{"sections":[
            {"type":"experience","entries":[
                {"position":"Keep","period":{"start":{"year":2020,"month":13},
                    "end":{"year":0}}},
                {"position":"Current","period":{"isCurrent":true,
                    "start":{"year":2021},"end":{"year":2022}}}
            ]}
        ]}}"""
        val entries = assertIs<ResumeExperienceSection>(
            ResumeImportJson.decode(document).sections.single(),
        ).entries
        assertEquals("Keep", entries[0].position)
        assertEquals(ResumeDate(2020), entries[0].period.start)
        assertEquals(null, entries[0].period.end)
        assertEquals(true, entries[1].period.isCurrent)
        assertEquals(null, entries[1].period.end)
    }

    @Test
    fun partialAndLegacyDocumentsKeepDefaultsAndMalformedDocumentsFail() {
        assertEquals(
            ResumeContent(),
            ResumeImportJson.decode("""{"formatVersion":1,"content":{}}"""),
        )
        val legacy = """{"formatVersion":1,"content":{
            "consentStatement":"Consent","consentStatementVisible":false
        }}"""
        assertEquals(ResumeConsent("Consent", false), ResumeImportJson.decode(legacy).consent)
        assertFailsWith<SerializationException> { ResumeImportJson.decode("not json") }
        listOf("[]", "{}", """{"formatVersion":2,"content":{}}""").forEach { value ->
            assertFailsWith<IllegalArgumentException> { ResumeImportJson.decode(value) }
        }
    }
}
