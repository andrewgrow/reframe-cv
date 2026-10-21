package reframecv.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import reframecv.documents.resume.ResumePrintKind
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.export.fixtures.hiddenResumeSections
import reframecv.export.fixtures.printableResumeFixture

class ResumePrintMapperTest {
    @Test
    fun includesStructuredContentAndPreservesSectionOrderWithoutInternalMetadata() {
        val data = printableResumeFixture().toPrintData(null)
        val text = (data.header + data.items).joinToString("\n") { it.text }
        listOf(
            "Alex Taylor", "Remote or hybrid", "Authorized to work", "Application 1",
            "Field application 1", "Language: Kotlin", "Graduated with honors", "2011 - 2015",
            "English - C1", "Community", "processing of my data",
        ).forEach { assertTrue(it in text, it) }
        listOf("Internal resume name", "Internal search keyword", "Hidden confidential text")
            .forEach { assertFalse(it in text, it) }
        val detail = data.header.single {
            it.kind == ResumePrintKind.Detail
        }
        assertTrue(detail.text.endsWith("Remote or hybrid"))
        assertEquals(
            ResumePrintKind.Headline,
            data.header.last().kind,
        )
        assertEquals(ResumePrintKind.Consent, data.items.last().kind)
        assertTrue(data.items.any { it.kind == ResumePrintKind.Link })
        assertTrue(text.indexOf("Professional summary") < text.indexOf("Work experience"))
    }

    @Test
    fun unnamedSummaryStillStartsBelowTheHeaderDivider() {
        val original = printableResumeFixture()
        val summary = original.content.sections
            .filterIsInstance<ResumeSummarySection>()
            .first().copy(title = "")
        val data = original.copy(
            content = original.content.copy(sections = listOf(summary)),
        ).toPrintData(null)
        assertTrue(data.items.first().ruleBefore)
    }

    @Test
    fun everySectionFlagAndConsentAndPortraitAreAppliedBeforeRendering() {
        val original = printableResumeFixture()
        val record = original.copy(
            photoVisible = false,
            content = original.content.copy(
                sections = hiddenResumeSections(),
                consent = original.content.consent.copy(visible = false),
            ),
        )
        val result = record.toPrintData(byteArrayOf(1))
        assertEquals(null, result.photo)
        assertTrue(result.items.isEmpty())
        assertTrue(result.contacts.isEmpty())
        assertTrue(result.header.any { "Authorized to work" in it.text })
    }
}
