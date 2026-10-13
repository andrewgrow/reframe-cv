package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_main
import reframecv.shared.generated.resources.resume_main_hint
import reframecv.ui.components.projects.resumes.editor.ResumeRole
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.RoleField(role: ResumeRole, onChange: (ResumeRole) -> Unit) {
    Column {
        Toggle(role == ResumeRole.Main, Res.string.resume_main) {
            onChange(if (it) ResumeRole.Main else ResumeRole.Regular)
        }
        SelectableText(
            stringResource(Res.string.resume_main_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
    }
}
