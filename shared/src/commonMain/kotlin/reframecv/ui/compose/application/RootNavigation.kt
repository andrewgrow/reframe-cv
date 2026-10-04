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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_projects_list
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.Spacing

private const val NAVIGATION_WIDTH = 220

@Composable
internal fun RootNavigation(onProjectsList: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.width(NAVIGATION_WIDTH.dp).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .safeContentPadding()
            .padding(Spacing.medium),
    ) {
        FillButton(
            text = stringResource(Res.string.navigation_projects_list),
            onClick = onProjectsList,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
