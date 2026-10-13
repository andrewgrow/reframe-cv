package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.runtime.Composable
import reframecv.domain.models.resume.content.ResumeProfile
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_headline
import reframecv.shared.generated.resources.resume_location
import reframecv.shared.generated.resources.resume_profile
import reframecv.shared.generated.resources.resume_profile_name

@Composable
internal fun ResumeFormScope.ProfileFields(
    profile: ResumeProfile,
    onChange: (ResumeProfile) -> Unit,
) {
    Heading(Res.string.resume_profile)
    Field(profile.name, Res.string.resume_profile_name, { onChange(profile.copy(name = it)) })
    Field(profile.headline, Res.string.resume_headline, { onChange(profile.copy(headline = it)) })
    Field(profile.location, Res.string.resume_location, { onChange(profile.copy(location = it)) })
}
