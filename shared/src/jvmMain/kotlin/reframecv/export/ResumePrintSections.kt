package reframecv.export

import reframecv.documents.resume.ResumePrintItem
import reframecv.documents.resume.ResumePrintKind
import reframecv.domain.models.resume.content.ResumeContactsSection
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperience
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeLanguagesSection
import reframecv.domain.models.resume.content.ResumeProject
import reframecv.domain.models.resume.content.ResumeSection
import reframecv.domain.models.resume.content.ResumeSkillsSection
import reframecv.domain.models.resume.content.ResumeSoftSkillsSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.ResumeTextSection

internal fun printSection(section: ResumeSection): List<ResumePrintItem> {
    val body = when (section) {
        is ResumeContactsSection -> section.entries.filter { it.value.isNotBlank() }.map {
            ResumePrintItem(joinPrint(it.label, it.value, separator = ": "), url = contactUrl(it))
        }

        is ResumeSummarySection -> printSummary(section)

        is ResumeTextSection -> listOf(ResumePrintItem(section.text))

        is ResumeSoftSkillsSection -> section.items.filter(String::isNotBlank).map {
            ResumePrintItem(it, ResumePrintKind.Bullet)
        }

        is ResumeLanguagesSection -> section.entries.map {
            ResumePrintItem(joinPrint(it.language, it.level, separator = " - "))
        }

        is ResumeSkillsSection -> section.groups.map {
            ResumePrintItem(
                joinPrint(
                    it.title,
                    it.items.filter(String::isNotBlank).joinToString(", "),
                    separator = ": ",
                ),
                boldPrefixLength = if (it.title.isBlank()) 0 else it.title.trim().length + 1,
            )
        }

        is ResumeEducationSection -> section.entries.flatMap {
            listOf(
                ResumePrintItem(it.institution, ResumePrintKind.Subheading, keepWithNext = true),
                ResumePrintItem(joinPrint(it.degree, it.specialty), keepWithNext = true),
                ResumePrintItem(it.notes, keepWithNext = true),
                ResumePrintItem(printPeriod(it.period), ResumePrintKind.Subheading),
            )
        }

        is ResumeExperienceSection -> section.entries.flatMapIndexed { index, entry ->
            printExperience(entry).mapIndexed { itemIndex, item ->
                if (index == 0 && itemIndex == 0) item.copy(ruleBefore = false) else item
            }
        }
    }.filter { it.text.isNotBlank() }
    val title = section.title.takeIf { it.isNotBlank() && body.isNotEmpty() }?.let {
        listOf(ResumePrintItem(it, ResumePrintKind.Section, keepWithNext = true, ruleBefore = true))
    }.orEmpty()
    return title + body
}

private fun printExperience(entry: ResumeExperience): List<ResumePrintItem> {
    val company = joinPrint(printPeriod(entry.period), entry.company, separator = ": ")
    val role =
        entry.position + entry.location.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()
    return listOf(
        ResumePrintItem(
            company,
            ResumePrintKind.Subheading,
            keepWithNext = true,
            ruleBefore = true,
        ),
        ResumePrintItem(
            role,
            ResumePrintKind.Role,
            keepWithNext = true,
            italicSuffixStart = entry.position.length.takeIf { entry.location.isNotBlank() },
        ),
        ResumePrintItem(entry.description),
    ) + entry.achievements.filter(String::isNotBlank).map {
        ResumePrintItem(it, ResumePrintKind.Bullet)
    } + printLinks(entry.links) + entry.projects.flatMap(::printProject)
}

private fun printProject(project: ResumeProject): List<ResumePrintItem> = listOf(
    ResumePrintItem(project.name, ResumePrintKind.Subheading, keepWithNext = true),
    ResumePrintItem(project.description, keepWithNext = true),
    ResumePrintItem(
        project.skillsOrTools.map { joinPrint(it.category, it.name, separator = ": ") }
            .filter(String::isNotBlank).joinToString(", "),
        ResumePrintKind.Detail,
        keepWithNext = project.links.isNotEmpty(),
    ),
) + printLinks(project.links).mapIndexed { index, item ->
    item.copy(keepWithNext = index < project.links.lastIndex)
}

private fun printSummary(section: ResumeSummarySection) =
    section.text.replace("\r\n", "\n").split(Regex("\n[\t ]*\n"))
        .filter(String::isNotBlank).mapIndexed { index, text ->
            ResumePrintItem(
                text,
                if (index ==
                    0
                ) {
                    ResumePrintKind.SummaryLead
                } else {
                    ResumePrintKind.Body
                },
            )
        }
