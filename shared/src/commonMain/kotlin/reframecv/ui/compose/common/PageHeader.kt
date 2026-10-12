package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_back
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun PageHeader(title: String, onBack: () -> Unit, enabled: Boolean = true) {
    val tokens = ReframeTheme.tokens
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            enabled = enabled,
            modifier = Modifier.size(tokens.dimensions.iconButtonSize),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(Res.string.navigation_back),
                modifier = Modifier.size(tokens.dimensions.iconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        SelectableText(
            title,
            style = tokens.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
