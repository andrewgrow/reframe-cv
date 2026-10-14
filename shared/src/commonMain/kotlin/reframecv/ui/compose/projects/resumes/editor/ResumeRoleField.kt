package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.runtime.Composable
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_main
import reframecv.ui.components.projects.resumes.editor.ResumeRole

@Composable
internal fun ResumeFormScope.RoleField(role: ResumeRole, onChange: (ResumeRole) -> Unit) {
    Toggle(role == ResumeRole.Main, Res.string.resume_main) {
        onChange(if (it) ResumeRole.Main else ResumeRole.Regular)
    }
}
