package reframecv.ui.components.projects.resumes.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeDate
import reframecv.domain.models.resume.content.ResumeDateRange
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.ResumeTextSection
import reframecv.domain.models.resume.content.sampleResumeContent

class ResumeDraftTest {
    @Test
    fun editingDraftRestoresAllSectionsDatesAndRecordIdentity() {
        val original = Resume(
            id = 12,
            projectId = 7,
            name = "Mobile",
            createdAt = 10,
            updatedAt = 20,
            content = sampleResumeContent(),
            sourceResumeId = 11,
            keywords = listOf("Kotlin", "Compose"),
        )
        val draft = ResumeDraft.fromResume(original)
        assertEquals(original, draft.applyTo(original))
        assertEquals((1L..8L).toList(), draft.sections.map { it.id })
        assertEquals(9L, draft.addSection(ResumeTextSection("Extra")).sections.last().id)
        val changed = draft.copy(
            name = "  Backend  ",
            profile = draft.profile.copy(headline = "Engineer"),
        )
            .moveSection(8, -1).applyTo(original)
        assertEquals("Backend", changed.name)
        assertEquals("Engineer", changed.content.profile.headline)
        assertEquals(original.id, changed.id)
        assertEquals(original.projectId, changed.projectId)
        assertEquals(original.createdAt, changed.createdAt)
        assertEquals(original.sourceResumeId, changed.sourceResumeId)
        assertEquals("Certifications", changed.content.sections[6].title)
        assertEquals(original.content, sampleResumeContent())
    }

    @Test
    fun preservesAllStructuredContentInSchemaOrder() {
        val content = sampleResumeContent()
        var draft =
            ResumeDraft("  Android Developer  ", " Kotlin, Compose, Kotlin, , ", content.profile)
        content.sections.forEach { draft = draft.addSection(it) }
        val resume = draft.toResume(7)
        assertEquals(content, resume.content)
        assertEquals("Android Developer", resume.name)
        assertEquals(7L, resume.projectId)
        assertEquals(0L, resume.id)
        assertEquals(listOf("Kotlin", "Compose"), resume.keywords)
        assertEquals(null, resume.sourceResumeId)
    }

    @Test
    fun ordersSectionsWithoutChangingIdentityAndRemovesOnlySelectedSection() {
        val draft = ResumeDraft("Draft").addSection(ResumeSummarySection(text = "Summary"))
            .addSection(ResumeTextSection("Custom", visible = false, text = "Text"))
        val moved = draft.moveSection(2, -1)
        assertEquals(listOf(2L, 1L), moved.sections.map { it.id })
        assertFalse(moved.sections.first().value.visible)
        assertEquals(moved, moved.moveSection(2, -1))
        assertEquals(moved, moved.moveSection(99, 1))
        assertEquals(listOf(1L), moved.removeSection(2).sections.map { it.id })
        val added = moved.removeSection(2).addSection(ResumeSummarySection())
        assertEquals(3L, added.sections.last().id)
    }

    @Test
    fun permitsPartialContentButRejectsBlankNameAndInvalidDates() {
        assertTrue(ResumeDraft("Draft").isValid)
        assertFalse(ResumeDraft("  ").isValid)
        assertFailsWith<IllegalArgumentException> { ResumeDraft().toResume(1) }
        val content = sampleResumeContent()
        val section = ResumeSectionDraft(
            1,
            content.sections.filterIsInstance<ResumeEducationSection>().single(),
        )
            .copy(periods = listOf(ResumePeriodDraft("2020-13")))
        val draft = ResumeDraft("Draft", sections = listOf(section))
        assertFalse(draft.isValid)
        assertFailsWith<IllegalArgumentException> { draft.toResume(1) }
        assertTrue(
            draft.changeSection(section.copy(periods = listOf(ResumePeriodDraft("2020")))).isValid,
        )
        assertFalse(section.copy(periods = emptyList()).isValid)
    }

    @Test
    fun datesSupportYearsMonthsMissingDatesAndPresentWithoutLosingTypedEnd() {
        listOf("", " ", "0001", "9999", "2020-01", "2020-12", " 2020 ").forEach {
            assertTrue(ResumePeriodDraft.validDate(it), it)
        }
        listOf(
            "2", "202", "2020-", "2020-1", "0000", "10000", "2020-00", "2020-13",
            "year", "2020-01-01",
        ).forEach {
            assertFalse(ResumePeriodDraft.validDate(it), it)
        }
        assertEquals(
            ResumeDateRange(ResumeDate(2020), ResumeDate(2024, 6)),
            ResumePeriodDraft("2020", "2024-06").toPeriod(),
        )
        val ongoing = ResumePeriodDraft("2020-09", "invalid hidden end", true)
        assertTrue(ongoing.isValid)
        assertEquals(ResumeDateRange(ResumeDate(2020, 9), isCurrent = true), ongoing.toPeriod())
        assertFalse(ongoing.copy(isCurrent = false).isValid)
        assertEquals(ResumeDateRange(), ResumePeriodDraft().toPeriod())
    }
}
