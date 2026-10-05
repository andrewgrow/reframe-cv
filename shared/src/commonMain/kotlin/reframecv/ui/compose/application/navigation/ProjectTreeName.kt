package reframecv.ui.compose.application.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun ProjectTreeName(
    name: String,
    selected: Boolean,
    hasChildren: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    interaction: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    var truncated by remember(name) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
        ),
        tooltip = { if (truncated) PlainTooltip { Text(name) } },
        state = rememberTooltipState(),
        enableUserInput = truncated,
        modifier = modifier,
    ) {
        Text(
            text = name,
            color = if (selected) colors.onSecondaryContainer else colors.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { truncated = it.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth()
                .onKeyEvent { event ->
                    handleTreeKey(event, focusManager, hasChildren, expanded, onToggle)
                }
                .semantics { this.selected = selected }
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .padding(vertical = ReframeTheme.tokens.spacing.small),
        )
    }
}

private fun handleTreeKey(
    event: KeyEvent,
    focusManager: FocusManager,
    hasChildren: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Next)

        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Previous)

        Key.DirectionRight -> {
            if (hasChildren && !expanded) onToggle()
            hasChildren
        }

        Key.DirectionLeft -> {
            if (hasChildren && expanded) onToggle()
            hasChildren
        }

        else -> false
    }
}
