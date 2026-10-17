package reframecv.domain.models.resume.content

fun sampleResumeContent() = ResumeContent(
    profile = ResumeProfile("Alex Résumé", "Mobile Developer", "Warsaw"),
    sections = listOf(
        ResumeContactsSection(
            entries = listOf(ResumeContact("email", "Email", "alex@example.com")),
        ),
        ResumeSummarySection(
            title = "Profile",
            text = "Builds \"reliable\" apps.\nРаботает с Kotlin.",
        ),
        ResumeSkillsSection(
            groups = listOf(ResumeSkillGroup("Mobile", listOf("Kotlin", "Compose"))),
        ),
        ResumeExperienceSection(
            entries = listOf(
                ResumeExperience(
                    position = "Developer",
                    company = "Example",
                    location = "Remote",
                    period = ResumeDateRange(start = ResumeDate(2021, 9), isCurrent = true),
                    achievements = listOf("Reduced release time"),
                    projects = listOf(
                        ResumeProject(
                            name = "Banking app",
                            description = "Secure mobile banking",
                            skillsOrTools = listOf(ResumeSkillOrTool("Kotlin", "Language")),
                        ),
                    ),
                ),
            ),
        ),
        ResumeEducationSection(
            entries = listOf(
                ResumeEducation(
                    institution = "University",
                    degree = "Bachelor",
                    specialty = "Computer science",
                    period = ResumeDateRange(ResumeDate(2016), ResumeDate(2020, 6)),
                ),
            ),
        ),
        ResumeLanguagesSection(entries = listOf(ResumeLanguage("English", "C1"))),
        ResumeSoftSkillsSection(visible = false, items = listOf("Communication")),
        ResumeTextSection(title = "Certifications", text = "Custom qualification"),
    ),
)
