package reframecv.export

import kotlin.test.Test
import reframecv.documents.resume.DesktopResumePdfRenderer
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.export.fixtures.hiddenResumeSections
import reframecv.export.fixtures.printableResumeFixture
import reframecv.export.fixtures.printableResumePortrait
import reframecv.export.fixtures.printableTextResumeFixture
import reframecv.testing.PdfGoldenScreenshotTest

class ResumePdfScreenshotTest : PdfGoldenScreenshotTest() {
    @Test
    fun withPortraitAndAllSectionTypes() {
        val data = printableResumeFixture().toPrintData(printableResumePortrait())
        capturePdfGolden(DesktopResumePdfRenderer.render(data), expectedPages = 3)
    }

    @Test
    fun withoutPortraitAndWithUnnamedSummary() {
        val data = printableTextResumeFixture().toPrintData(printableResumePortrait())
        capturePdfGolden(DesktopResumePdfRenderer.render(data), expectedPages = 2)
    }

    @Test
    fun longHeadlineWrapsBesidePortraitAndHiddenSectionsAreOmitted() {
        val original = printableResumeFixture()
        val sections = hiddenResumeSections().map { section ->
            if (section is ResumeSummarySection) section.copy(visible = true) else section
        }
        val resume = original.copy(
            content = original.content.copy(
                profile = original.content.profile.copy(
                    headline = "Senior Android Developer | Kotlin & Jetpack Compose | " +
                        "Real-Time Communications & Connected Devices",
                ),
                sections = sections,
                consent = original.content.consent.copy(visible = false),
            ),
        )
        capturePdfGolden(
            DesktopResumePdfRenderer.render(resume.toPrintData(printableResumePortrait())),
            expectedPages = 1,
        )
    }
}
