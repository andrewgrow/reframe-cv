package reframecv.ui.compose.settings.runners

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fits columns to their headers and values while keeping large fields bounded. */
@Composable
internal fun runnerColumnWidths(
    keys: List<String>,
    rows: List<Map<String, String>>,
): Map<String, Dp> {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val header = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    val body = MaterialTheme.typography.bodyMedium
    return remember(keys, rows, density, measurer, header, body) {
        val padding = with(density) { 16.dp.roundToPx() }
        val maximum = with(density) { 320.dp.roundToPx() } - padding
        val constraints = Constraints(maxWidth = maximum)
        keys.associateWith { key ->
            val heading = measurer.measure(
                key,
                header,
                softWrap = false,
                maxLines = 1,
                constraints = constraints,
            ).size.width
            val content = rows.maxOfOrNull { row ->
                measurer.measure(
                    row[key].orEmpty().replace(Regex("\\s+"), " ").trim(),
                    body,
                    softWrap = false,
                    maxLines = 1,
                    constraints = constraints,
                ).size.width
            } ?: 0
            with(density) { (maxOf(heading, content) + padding).toDp() }.coerceIn(100.dp, 320.dp)
        }
    }
}
