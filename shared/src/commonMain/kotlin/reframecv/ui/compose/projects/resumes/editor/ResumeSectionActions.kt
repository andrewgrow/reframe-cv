package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_collapse
import reframecv.shared.generated.resources.resume_down
import reframecv.shared.generated.resources.resume_expand
import reframecv.shared.generated.resources.resume_up
import reframecv.shared.generated.resources.resume_visible
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.SectionActions(
    removeLabel: StringResource,
    expanded: Boolean,
    visible: Boolean,
    first: Boolean,
    last: Boolean,
    onExpand: () -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val tokens = ReframeTheme.tokens
    val controls: @Composable () -> Unit = {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
            Action(if (expanded) Res.string.resume_collapse else Res.string.resume_expand, onExpand)
            Action(Res.string.resume_up, { onMove(-1) }, !first)
            Action(Res.string.resume_down, { onMove(1) }, !last)
            Toggle(visible, Res.string.resume_visible, onVisibilityChange)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= tokens.dimensions.resumeSectionControlsMinWidth) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.weight(1f)) { controls() }
                Action(removeLabel, onRemove)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
                controls()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Action(removeLabel, onRemove)
                }
            }
        }
    }
}
