package reframecv.documents.coverletter.fixtures

import reframecv.documents.PrintLink
import reframecv.documents.coverletter.CoverLetterPrintData

/** Provides fictional documents for repeatable rendering and extraction checks. */
internal object CoverLetterFixtures {
    private const val LONG_PARAGRAPH_COUNT = 20
    val links = listOf(
        PrintLink("Portfolio: example.com", "https://example.com"),
        PrintLink("Email: alex@example.com", "mailto:alex@example.com"),
    )
    val short = CoverLetterPrintData(
        """
        Dear hiring team,

        I am applying for the Mobile Developer position at ExampleCo. My experience includes Kotlin, Jetpack Compose, accessible interfaces and reliable offline workflows. I enjoy turning complex requirements into clear, maintainable applications.

        In my recent work, I improved application stability, introduced automated tests and worked closely with designers and backend engineers. I would welcome the opportunity to bring that experience to your team.

        International collaboration matters to me: Grüße aus München. Bonjour, équipe. Привет, команда. I value thoughtful communication, ownership and attention to detail.

        Thank you for considering my application. I would be happy to discuss how my experience could support your product.

        Kind regards,
        Alex Taylor
        """.trimIndent(),
        links,
    )

    fun paragraph(index: Int) =
        """Experience $index. I delivered Kotlin applications with Jetpack Compose, improved testing and supported reliable releases. I collaborated with designers and backend engineers to improve accessibility, performance and offline behavior. This work combined clear communication with careful technical decisions. End of experience $index."""

    val long = CoverLetterPrintData(
        (
            listOf("Dear hiring team,") + (1..LONG_PARAGRAPH_COUNT).map(::paragraph) +
                "Kind regards,\nAlex Taylor"
            ).joinToString("\n\n"),
        links,
    )
}
