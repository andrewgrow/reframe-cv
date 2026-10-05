package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import reframecv.shared.SelectableText
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun DashboardSection(
    title: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = ReframeTheme.tokens
    Column(
        modifier.background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(
                horizontal = tokens.spacing.medium,
                vertical = tokens.spacing.small + tokens.outlineWidth,
            ),
    ) {
        SelectableText(title, color = contentColor, style = MaterialTheme.typography.titleMedium)
    }
}
