package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_visible
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumePhotoVisibility(value: Boolean, enabled: Boolean, changed: (Boolean) -> Unit) {
    Row(
        Modifier.testTag("resume.photo.visibility")
            .toggleable(value, enabled = enabled, role = Role.Checkbox, onValueChange = changed),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Checkbox(value, onCheckedChange = null, enabled = enabled)
        Text(
            stringResource(Res.string.resume_visible),
            style = ReframeTheme.tokens.typography.bodySmall,
        )
    }
}
