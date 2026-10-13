package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.resume.content.ResumeContactsSection
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeLanguagesSection
import reframecv.domain.models.resume.content.ResumeSection
import reframecv.domain.models.resume.content.ResumeSkillsSection
import reframecv.domain.models.resume.content.ResumeSoftSkillsSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.ResumeTextSection
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_add_section
import reframecv.shared.generated.resources.resume_contacts
import reframecv.shared.generated.resources.resume_custom_text
import reframecv.shared.generated.resources.resume_education
import reframecv.shared.generated.resources.resume_experience
import reframecv.shared.generated.resources.resume_languages
import reframecv.shared.generated.resources.resume_skills
import reframecv.shared.generated.resources.resume_soft_skills
import reframecv.shared.generated.resources.resume_summary
import reframecv.ui.compose.common.FillButton

@Composable
internal fun ResumeSectionPicker(enabled: Boolean, onAdd: (ResumeSection) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val choices = listOf(
        Res.string.resume_summary to { title: String -> ResumeSummarySection(title) },
        Res.string.resume_skills to { title: String -> ResumeSkillsSection(title) },
        Res.string.resume_experience to { title: String -> ResumeExperienceSection(title) },
        Res.string.resume_education to { title: String -> ResumeEducationSection(title) },
        Res.string.resume_languages to { title: String -> ResumeLanguagesSection(title) },
        Res.string.resume_soft_skills to { title: String -> ResumeSoftSkillsSection(title) },
        Res.string.resume_contacts to { title: String -> ResumeContactsSection(title) },
        Res.string.resume_custom_text to { title: String -> ResumeTextSection(title) },
    )
    Box {
        FillButton(
            stringResource(Res.string.resume_add_section),
            { expanded = true },
            enabled = enabled,
            outlineState = FillButton.OutlineState.Visible,
        )
        DropdownMenu(expanded && enabled, { expanded = false }) {
            choices.forEach { (label, create) ->
                val title = stringResource(label)
                DropdownMenuItem(text = { androidx.compose.material3.Text(title) }, onClick = {
                    expanded = false
                    onAdd(create(title))
                })
            }
        }
    }
}
