package reframecv.ui.compose.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.settings_title
import reframecv.ui.components.application.navigation.ProjectTreeComponent
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RootNavigation(
    component: ProjectTreeComponent,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
    settingsSelected: Boolean = false,
) {
    val tokens = ReframeTheme.tokens
    Column(
        modifier.width(
            tokens.dimensions.navigationWidth,
        ).background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        ProjectNavigationTree(component, Modifier.weight(1f), settingsSelected)
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().padding(tokens.spacing.medium)
                .background(
                    if (settingsSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    MaterialTheme.shapes.small,
                )
                .selectable(settingsSelected, role = Role.Button, onClick = onSettings)
                .padding(tokens.spacing.small).testTag("navigation.settings"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = null,
                Modifier.size(tokens.dimensions.iconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(Res.string.settings_title),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
