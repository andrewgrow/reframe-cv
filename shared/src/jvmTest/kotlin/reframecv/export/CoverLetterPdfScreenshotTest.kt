package reframecv.export

import kotlin.test.Test
import reframecv.documents.PrintLink
import reframecv.documents.coverletter.CoverLetterPrintData
import reframecv.documents.coverletter.DesktopCoverLetterPdfRenderer
import reframecv.testing.PdfGoldenScreenshotTest

class CoverLetterPdfScreenshotTest : PdfGoldenScreenshotTest() {
    private val links = listOf(
        PrintLink("Portfolio: example.com", "https://example.com"),
        PrintLink("Email: alex@example.com", "mailto:alex@example.com"),
    )

    @Test
    fun singlePageLetterWithLinksAndInternationalText() {
        val text = """
            Dear hiring team,

            I am applying for the Mobile Developer position at ExampleCo. I build Kotlin applications with Jetpack Compose, accessible interfaces and reliable offline workflows.

            In my recent work, I improved application stability, introduced automated tests and collaborated with designers and backend engineers. I would welcome the opportunity to bring that experience to your team.

            International collaboration matters to me: Grüße aus München. Bonjour, équipe. Привет, команда.

            Thank you for considering my application.

            Kind regards,
            Alex Taylor
        """.trimIndent()
        capturePdfGolden(
            DesktopCoverLetterPdfRenderer.render(CoverLetterPrintData(text, links)),
            expectedPages = 1,
        )
    }

    @Test
    fun multiplePagesKeepParagraphsAndFootersReadable() {
        val paragraphs = (1..12).map { index ->
            "Experience $index. I delivered Kotlin applications with Jetpack Compose, " +
                "improved testing and supported reliable releases. I collaborated with " +
                "designers and backend engineers to improve accessibility, performance " +
                "and offline behavior. This work combined clear communication with " +
                "careful technical decisions. End of experience $index."
        }
        val text = (listOf("Dear hiring team,") + paragraphs + "Kind regards,\nAlex Taylor")
            .joinToString("\n\n")
        capturePdfGolden(
            DesktopCoverLetterPdfRenderer.render(CoverLetterPrintData(text, links)),
            expectedPages = 2,
        )
    }
}
