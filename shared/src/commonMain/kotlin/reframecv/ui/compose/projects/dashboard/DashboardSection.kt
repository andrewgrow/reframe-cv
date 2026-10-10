package reframecv.ui.compose.projects.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun DashboardSection(
    title: String,
    entries: List<DashboardEntry>,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val tokens = ReframeTheme.tokens
    Column(
        modifier.then(
            onClick?.let {
                Modifier.clip(
                    MaterialTheme.shapes.medium,
                ).clickable(role = Role.Button, onClick = it)
            } ?: Modifier,
        )
            .testTag("dashboard.section.$title")
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(
                horizontal = tokens.spacing.medium,
                vertical = tokens.spacing.small + tokens.outlineWidth,
            ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            DashboardSectionText(
                title,
                selectionModifier = Modifier.weight(1f),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                selectable = onClick == null,
            )
            DashboardSectionText(
                entries.size.toString(),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                selectable = onClick == null,
            )
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("dashboard.records.$title"),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        ) {
            items(entries, key = { it.id }) { entry ->
                DashboardSectionText(
                    entry.name,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge,
                    selectable = onClick == null,
                )
            }
        }
    }
}
