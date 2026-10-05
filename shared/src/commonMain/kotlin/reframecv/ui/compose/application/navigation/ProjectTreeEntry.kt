package reframecv.ui.compose.application.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_collapse
import reframecv.shared.generated.resources.navigation_expand
import reframecv.ui.theme.ReframeTheme

@Composable
@Suppress("LongParameterList")
internal fun ProjectTreeEntry(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    hasChildren: Boolean = false,
    expanded: Boolean = false,
    onToggle: () -> Unit = {},
) {
    val tokens = ReframeTheme.tokens
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val arrowInteraction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val background = when {
        selected -> colors.secondaryContainer
        hovered -> colors.surfaceContainerHighest
        else -> Color.Transparent
    }
    Row(
        modifier.fillMaxWidth()
            .padding(horizontal = tokens.spacing.small, vertical = tokens.outlineWidth)
            .background(background, MaterialTheme.shapes.small)
            .hoverable(interaction)
            .border(
                tokens.outlineWidth,
                if (focused) colors.primary else Color.Transparent,
                MaterialTheme.shapes.small,
            )
            .padding(start = tokens.spacing.medium * depth, end = tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(tokens.dimensions.iconSize), contentAlignment = Alignment.Center) {
            if (hasChildren) {
                ProjectTreeArrow(name, expanded, arrowInteraction, onToggle)
            }
        }
        ProjectTreeName(
            name = name,
            selected = selected,
            hasChildren = hasChildren,
            expanded = expanded,
            onToggle = onToggle,
            onClick = onClick,
            interaction = interaction,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ProjectTreeArrow(
    name: String,
    expanded: Boolean,
    interaction: MutableInteractionSource,
    onToggle: () -> Unit,
) {
    Icon(
        imageVector = if (expanded) {
            Icons.Outlined.KeyboardArrowDown
        } else {
            Icons.AutoMirrored.Outlined.KeyboardArrowRight
        },
        contentDescription = stringResource(
            if (expanded) Res.string.navigation_collapse else Res.string.navigation_expand,
            name,
        ),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.focusProperties { canFocus = false }.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onToggle,
        ),
    )
}
