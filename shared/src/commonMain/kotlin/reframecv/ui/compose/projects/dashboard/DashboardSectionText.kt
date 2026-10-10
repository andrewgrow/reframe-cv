package reframecv.ui.compose.projects.dashboard

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import reframecv.shared.SelectableText

/** Text inside an interactive section lets its parent handle mouse and keyboard clicks. */
@Composable
internal fun DashboardSectionText(
    text: String,
    color: Color,
    style: TextStyle,
    selectable: Boolean,
    selectionModifier: Modifier = Modifier,
) {
    if (selectable) {
        SelectableText(text, color = color, style = style, selectionModifier = selectionModifier)
    } else {
        Text(text, modifier = selectionModifier, color = color, style = style)
    }
}
