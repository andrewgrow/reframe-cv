package reframecv.ui.compose.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.section_collapse
import reframecv.shared.generated.resources.section_expand
import reframecv.ui.components.common.section.CollapsibleSectionState
import reframecv.ui.theme.ReframeTheme

/** Groups related fields while keeping their saved UI state during animated collapse and expansion. */
@Composable
internal fun CollapsibleSection(
    title: String,
    state: CollapsibleSectionState,
    toggle: () -> Unit,
    tag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = ReframeTheme.tokens
    val saved = rememberSaveableStateHolder()
    OutlinedCard(
        Modifier.fillMaxWidth().testTag(tag),
        border = BorderStroke(tokens.outlineWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().testTag("$tag.toggle")
                .clickable(enabled = !state.loading, role = Role.Button, onClick = toggle)
                .padding(tokens.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(
                if (state.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (state.expanded) Res.string.section_collapse else Res.string.section_expand,
                ),
            )
        }
        // Create the transition only after restoration, so its initial state is already settled.
        if (!state.loading) {
            AnimatedVisibility(
                visible = state.expanded,
                enter = expandVertically(tween(tokens.fillAnimationMillis)) + fadeIn(),
                exit = shrinkVertically(tween(tokens.fillAnimationMillis)) + fadeOut(),
            ) {
                saved.SaveableStateProvider(tag) {
                    Column(
                        Modifier.fillMaxWidth().padding(
                            start = tokens.spacing.medium,
                            end = tokens.spacing.medium,
                            bottom = tokens.spacing.medium,
                        ),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                        content = content,
                    )
                }
            }
        }
    }
}
