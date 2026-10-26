package reframecv.export

import kotlin.test.Test
import reframecv.documents.vacancy.DesktopVacancyPdfRenderer
import reframecv.documents.vacancy.VacancyPrintData
import reframecv.testing.PdfGoldenScreenshotTest

class VacancyPdfScreenshotTest : PdfGoldenScreenshotTest() {
    private val data = VacancyPrintData(
        title = "Senior Android Engineer - Mobile Design Systems",
        company = "Example Systems",
        url = "https://example.test/careers/android-engineer",
        description =
            "Join our mobile team to build accessible applications with Kotlin and Compose.\n\n" +
                "Work with an international team: Grüße aus München. Bonjour, équipe. Привет, команда.",
        responsibilities = listOf(
            "Develop reusable UI components and document their APIs.",
            "Collaborate with designers, engineers and product teams.",
        ),
        requirements = listOf(
            "Production experience with Kotlin and Jetpack Compose.",
            "Automated testing and accessible UI development.",
        ),
        niceToHave = listOf("Experience maintaining a shared design system."),
    )

    @Test
    fun singlePageKeepsHierarchyBulletsAndSourceLinkReadable() {
        capturePdfGolden(DesktopVacancyPdfRenderer.render(data), expectedPages = 1)
    }

    @Test
    fun multiplePagesKeepSectionHeadingsTextAndPageNumbersReadable() {
        val long = data.copy(
            description = (1..12).joinToString("\n\n") { index ->
                "Team context $index. We build Kotlin applications with Jetpack Compose, " +
                    "automated testing and accessible interfaces. Engineers collaborate with " +
                    "designers and product teams to improve reliability, maintain reusable " +
                    "components and document their decisions. End of context $index."
            },
        )
        capturePdfGolden(DesktopVacancyPdfRenderer.render(long), expectedPages = 2)
    }
}
