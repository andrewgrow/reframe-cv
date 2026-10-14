package reframecv.ui.compose.projects.resumes.editor

import org.jetbrains.compose.resources.StringResource
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
import reframecv.shared.generated.resources.resume_remove_contacts_section
import reframecv.shared.generated.resources.resume_remove_education_section
import reframecv.shared.generated.resources.resume_remove_experience_section
import reframecv.shared.generated.resources.resume_remove_languages_section
import reframecv.shared.generated.resources.resume_remove_skills_section
import reframecv.shared.generated.resources.resume_remove_soft_skills_section
import reframecv.shared.generated.resources.resume_remove_summary_section
import reframecv.shared.generated.resources.resume_remove_text_section

internal fun ResumeSection.removeLabel(): StringResource = when (this) {
    is ResumeSummarySection -> Res.string.resume_remove_summary_section
    is ResumeSkillsSection -> Res.string.resume_remove_skills_section
    is ResumeExperienceSection -> Res.string.resume_remove_experience_section
    is ResumeEducationSection -> Res.string.resume_remove_education_section
    is ResumeLanguagesSection -> Res.string.resume_remove_languages_section
    is ResumeSoftSkillsSection -> Res.string.resume_remove_soft_skills_section
    is ResumeContactsSection -> Res.string.resume_remove_contacts_section
    is ResumeTextSection -> Res.string.resume_remove_text_section
}
