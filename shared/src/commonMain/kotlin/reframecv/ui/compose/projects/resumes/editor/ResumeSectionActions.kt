package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_collapse
import reframecv.shared.generated.resources.resume_down
import reframecv.shared.generated.resources.resume_expand
import reframecv.shared.generated.resources.resume_remove
import reframecv.shared.generated.resources.resume_up
import reframecv.shared.generated.resources.resume_visible
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.SectionActions(
    expanded: Boolean,
    visible: Boolean,
    first: Boolean,
    last: Boolean,
    onExpand: () -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        ) {
            Action(if (expanded) Res.string.resume_collapse else Res.string.resume_expand, onExpand)
            Action(Res.string.resume_up, { onMove(-1) }, !first)
            Action(Res.string.resume_down, { onMove(1) }, !last)
            Toggle(visible, Res.string.resume_visible, onVisibilityChange)
        }
        Action(Res.string.resume_remove, onRemove)
    }
}
