package reframecv.ui.compose.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import reframecv.ui.theme.ReframeTheme

/** Keeps long form content beside shared scrollbar and page-end controls. */
@Composable
internal fun FastScrollColumn(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Column(
            contentModifier.weight(1f).verticalScroll(scrollState),
            verticalArrangement = verticalArrangement,
            content = content,
        )
        FastScrollRail(scrollState)
    }
}
