package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.dashboard_load_error
import reframecv.shared.generated.resources.navigation_retry
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun DashboardLoadError(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(ReframeTheme.tokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(
            ReframeTheme.tokens.spacing.medium,
            Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SelectableText(
            stringResource(Res.string.dashboard_load_error),
            color = ReframeTheme.colorScheme.critical,
            style = MaterialTheme.typography.bodyLarge,
        )
        FillButton(
            text = stringResource(Res.string.navigation_retry),
            onClick = onRetry,
            outlineState = FillButton.OutlineState.Visible,
        )
    }
}
