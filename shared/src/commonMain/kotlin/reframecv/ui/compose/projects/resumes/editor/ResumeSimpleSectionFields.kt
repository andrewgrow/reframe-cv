package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import reframecv.domain.models.resume.content.ResumeContactsSection
import reframecv.domain.models.resume.content.ResumeLanguagesSection
import reframecv.domain.models.resume.content.ResumeSection
import reframecv.domain.models.resume.content.ResumeSkillsSection
import reframecv.domain.models.resume.content.ResumeSoftSkillsSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.ResumeTextSection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_items
import reframecv.shared.generated.resources.resume_text

@Composable
internal fun ResumeFormScope.SimpleSectionFields(
    section: ResumeSection,
    onChange: (ResumeSection) -> Unit,
) {
    when (section) {
        is ResumeSummarySection -> ResizableField(
            section.text,
            Res.string.resume_text,
            "resume.summary",
        ) {
            onChange(section.copy(text = it))
        }

        is ResumeTextSection -> Field(section.text, Res.string.resume_text, {
            onChange(section.copy(text = it))
        }, multiline = true)

        is ResumeSoftSkillsSection -> CommaSeparatedItemsField(section.items) {
            onChange(section.copy(items = it))
        }

        is ResumeSkillsSection -> SkillGroupFields(section.groups) {
            onChange(section.copy(groups = it))
        }

        is ResumeLanguagesSection -> LanguageFields(section.entries) {
            onChange(section.copy(entries = it))
        }

        is ResumeContactsSection -> ContactFields(section.entries) {
            onChange(section.copy(entries = it))
        }

        else -> error("Work and education sections have dedicated editors")
    }
}

@Composable
internal fun ResumeFormScope.CommaSeparatedItemsField(
    items: List<String>,
    onChange: (List<String>) -> Unit,
) {
    var input by remember { mutableStateOf(items.joinToString(", ") { it.trim() }) }
    LaunchedEffect(items) {
        if (input.split(',') != items) {
            input = items.joinToString(", ") { it.trim() }
        }
    }
    Field(input, Res.string.resume_items, {
        input = it
        onChange(it.split(','))
    }, multiline = true, minLines = 1)
}
