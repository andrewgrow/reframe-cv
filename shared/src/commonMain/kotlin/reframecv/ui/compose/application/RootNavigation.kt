package reframecv.ui.compose.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_projects_list
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun RootNavigation(onProjectsList: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.width(ReframeTheme.tokens.dimensions.navigationWidth).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .safeContentPadding()
            .padding(ReframeTheme.tokens.spacing.medium),
    ) {
        FillButton(
            text = stringResource(Res.string.navigation_projects_list),
            onClick = onProjectsList,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
