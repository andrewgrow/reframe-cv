package reframecv.ui.compose.application

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_show
import reframecv.shared.generated.resources.settings_title
import reframecv.ui.components.application.navigation.ProjectTreeComponent
import reframecv.ui.components.common.section.CollapsibleSectionComponent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RootNavigation(
    component: ProjectTreeComponent,
    panel: CollapsibleSectionComponent,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
    settingsSelected: Boolean = false,
    footerHeight: Dp? = null,
) {
    val tokens = ReframeTheme.tokens
    val state by panel.state.subscribeAsState()
    if (state.loading) return
    val collapsed = !state.expanded
    val width by animateDpAsState(
        if (collapsed) {
            tokens.dimensions.iconSize + tokens.spacing.small * 2 + tokens.spacing.medium * 2
        } else {
            tokens.dimensions.navigationWidth
        },
        animationSpec = tween(tokens.fillAnimationMillis),
    )
    Column(
        modifier.width(width).clipToBounds().testTag("navigation.panel")
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(Modifier.weight(1f)) {
            if (collapsed) {
                IconButton(
                    onClick = panel::onToggle,
                    modifier = Modifier.padding(tokens.spacing.medium)
                        .size(tokens.dimensions.iconSize + tokens.spacing.small * 2)
                        .testTag("navigation.expand"),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        stringResource(Res.string.navigation_show),
                        Modifier.size(tokens.dimensions.iconSize),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                ProjectNavigationTree(
                    component,
                    settingsSelected = settingsSelected,
                    onCollapse = panel::onToggle,
                )
            }
        }
        NavigationSettings(onSettings, settingsSelected, collapsed, footerHeight)
    }
}

@Composable
private fun NavigationSettings(
    onSettings: () -> Unit,
    selected: Boolean,
    collapsed: Boolean,
    footerHeight: Dp?,
) {
    val tokens = ReframeTheme.tokens
    val title = stringResource(Res.string.settings_title)
    Box(
        Modifier.fillMaxWidth().heightIn(
            min =
                footerHeight ?: (tokens.dimensions.projectActionHeight + tokens.spacing.medium * 2),
        ).testTag("navigation.footer"),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(tokens.spacing.medium)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    MaterialTheme.shapes.small,
                )
                .selectable(selected, role = Role.Button, onClick = onSettings)
                .padding(tokens.spacing.small).testTag("navigation.settings"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = if (collapsed) title else null,
                Modifier.size(tokens.dimensions.iconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            if (!collapsed) Text(title, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
