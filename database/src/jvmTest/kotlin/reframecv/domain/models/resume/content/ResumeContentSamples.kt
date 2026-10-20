package reframecv.domain.models.resume.content

fun sampleResumeContent() = ResumeContent(
    profile = ResumeProfile(
        "Alex Résumé",
        "Mobile Developer",
        "Warsaw",
        workArrangement = "Remote or hybrid",
        workAuthorization = "Eligible to work in Austria",
    ),
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
                    description = "Builds production apps.\nWorks across teams.",
                    links = listOf(ResumeLink("Google Play", "https://example.com/app")),
                    achievements = listOf("Reduced release time"),
                    projects = listOf(
                        ResumeProject(
                            name = "Banking app",
                            description = "Secure mobile banking",
                            links = listOf(ResumeLink("Project", "https://example.com/project")),
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
                    notes = "Graduated with honors.\nFaculty award.",
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
