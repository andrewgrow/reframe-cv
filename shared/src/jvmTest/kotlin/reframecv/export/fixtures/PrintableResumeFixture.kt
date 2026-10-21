package reframecv.export.fixtures

import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeConsent
import reframecv.domain.models.resume.content.ResumeContact
import reframecv.domain.models.resume.content.ResumeContactsSection
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeDate
import reframecv.domain.models.resume.content.ResumeDateRange
import reframecv.domain.models.resume.content.ResumeEducation
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperience
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeLanguage
import reframecv.domain.models.resume.content.ResumeLanguagesSection
import reframecv.domain.models.resume.content.ResumeLink
import reframecv.domain.models.resume.content.ResumeProfile
import reframecv.domain.models.resume.content.ResumeProject
import reframecv.domain.models.resume.content.ResumeSkillGroup
import reframecv.domain.models.resume.content.ResumeSkillOrTool
import reframecv.domain.models.resume.content.ResumeSkillsSection
import reframecv.domain.models.resume.content.ResumeSoftSkillsSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.ResumeTextSection

internal fun printableResumeFixture() = Resume(
    12,
    7,
    "Internal resume name",
    0,
    0,
    keywords = listOf("Internal search keyword"),
    photoPath = "documents/resume-photos/test.png",
    content = ResumeContent(
        profile = profileFixture(),
        language = "EN",
        consent = ResumeConsent("I consent to the processing of my data for recruitment purposes."),
        sections = listOf(
            ResumeContactsSection(
                entries = listOf(
                    ResumeContact("email", "Email", "alex@example.com"),
                    ResumeContact("website", "Portfolio", "https://example.com"),
                ),
            ),
            ResumeSummarySection(
                title = "Professional summary",
                text =
                    "Android engineer building reliable Kotlin applications.\n\n" +
                        "Experienced in product delivery, " +
                        "accessible interfaces, testing and collaboration across distributed teams.",
            ),
            ResumeSkillsSection(
                groups = listOf(
                    ResumeSkillGroup(
                        "Development",
                        listOf("Kotlin", "Jetpack Compose", "Coroutines"),
                    ),
                    ResumeSkillGroup("Quality", listOf("JUnit", "UI testing", "CI/CD")),
                ),
            ),
            experienceFixture(),
            ResumeEducationSection(
                entries = listOf(
                    ResumeEducation(
                        "Example University",
                        "BSc",
                        "Computer Science",
                        ResumeDateRange(ResumeDate(2011), ResumeDate(2015)),
                        "Graduated with honors",
                    ),
                ),
            ),
            ResumeLanguagesSection(
                entries = listOf(ResumeLanguage("English", "C1"), ResumeLanguage("German", "B2")),
            ),
            ResumeSoftSkillsSection(items = listOf("Mentoring", "Technical communication")),
            ResumeTextSection("Community", text = "Volunteer mentor for early-career engineers."),
            ResumeTextSection(
                "Private section",
                visible = false,
                text = "Hidden confidential text",
            ),
        ),
    ),
)

internal fun hiddenResumeSections() = printableResumeFixture().content.sections.map { section ->
    when (section) {
        is ResumeContactsSection -> section.copy(visible = false)
        is ResumeSummarySection -> section.copy(visible = false)
        is ResumeExperienceSection -> section.copy(visible = false)
        is ResumeSkillsSection -> section.copy(visible = false)
        is ResumeEducationSection -> section.copy(visible = false)
        is ResumeLanguagesSection -> section.copy(visible = false)
        is ResumeSoftSkillsSection -> section.copy(visible = false)
        is ResumeTextSection -> section.copy(visible = false)
    }
}

private fun experienceFixture() = ResumeExperienceSection(
    entries = (1..5).map { index ->
        ResumeExperience(
            "Senior Android Developer $index",
            "ExampleCo $index",
            "Berlin",
            ResumeDateRange(ResumeDate(2015 + index, 2), ResumeDate(2016 + index, 10)),
            description = "Built mobile workflows for a collaborative product team. " +
                "Worked with design and backend engineers to deliver " +
                "maintainable applications.",
            achievements = listOf(
                "Delivered accessible offline-first screens with Kotlin and Jetpack Compose.",
                "Introduced automated regression coverage and improved release reliability.",
            ),
            links = listOf(
                ResumeLink("Application $index", "https://example.com/apps/$index"),
            ),
            projects = listOf(
                ResumeProject(
                    "Field application $index",
                    "A mobile workspace for teams operating with unreliable connectivity.",
                    listOf(
                        ResumeSkillOrTool("Kotlin", "Language"),
                        ResumeSkillOrTool("Room", "Storage"),
                    ),
                    listOf(
                        ResumeLink(
                            "Project $index",
                            "https://example.com/projects/$index",
                        ),
                    ),
                ),
            ),
        )
    },
)

private fun profileFixture() = ResumeProfile(
    "Alex Taylor",
    "Senior Android Developer",
    "Berlin, Germany",
    "Remote or hybrid",
    "Authorized to work in the EU",
)

internal fun printableTextResumeFixture(): Resume {
    val original = printableResumeFixture()
    val sections = original.content.sections.filterNot {
        it is ResumeTextSection || it is ResumeSoftSkillsSection || it is ResumeSkillsSection
    }.map { section ->
        when (section) {
            is ResumeSummarySection -> section.copy(title = "")
            is ResumeExperienceSection -> section.copy(entries = section.entries.take(3))
            else -> section
        }
    }
    return original.copy(photoVisible = false, content = original.content.copy(sections = sections))
}
