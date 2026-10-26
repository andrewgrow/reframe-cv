package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import reframecv.domain.models.providers.RunnerResult
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.compose.common.FastHorizontalScrollRail
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FastScrollRail
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.compose.common.SelectableLinkText

internal fun runnerCells(result: RunnerResult): Map<String, String> = try {
    when (val json = Json.parseToJsonElement(result.rawResultAsJson)) {
        is JsonObject -> json.mapValues { (_, value) ->
            if (value is JsonPrimitive) value.content else value.toString()
        }

        else -> mapOf("value" to json.toString())
    }
} catch (_: IllegalArgumentException) {
    mapOf("value" to result.rawResultAsJson)
}

@Composable
internal fun RunnerResultsTable(
    results: List<RunnerResult>,
    columns: List<String>,
    component: RunnerRunsComponent,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val rows = remember(results) { results.map(::runnerCells) }
    val keys = (columns + rows.flatMap { it.keys }).distinct()
    val widths = runnerColumnWidths(keys, rows)
    var cell by remember { mutableStateOf<Pair<String, String>?>(null) }
    Column(modifier.testTag("runner.results.table")) {
        TableHeader(horizontal, keys, widths)
        Row(Modifier.weight(1f)) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(vertical)) {
                results.forEachIndexed { index, result ->
                    Row(
                        Modifier.fillMaxWidth().height(96.dp).testTag("runner.result.${result.id}"),
                    ) {
                        Box(Modifier.width(ACTION_WIDTH).padding(vertical = 8.dp)) {
                            RunnerResultActions(result, component, enabled)
                        }
                        TableCells(horizontal, widths, rows[index]) { name, text ->
                            cell =
                                name to text
                        }
                    }
                    HorizontalDivider()
                }
            }
            FastScrollRail(vertical)
        }
        Row {
            Spacer(Modifier.width(ACTION_WIDTH))
            FastHorizontalScrollRail(horizontal)
        }
    }
    cell?.let { (name, text) ->
        ResizableDialog(
            onDismissRequest = { cell = null },
            title = { Text(name) },
            text = {
                FastScrollColumn(Modifier.height(300.dp)) {
                    SelectableLinkText(text, openUrl = uriHandler::openUri)
                }
            },
            actions = { androidx.compose.material3.TextButton({ cell = null }) { Text("Close") } },
            modifier = Modifier.testTag("runner.result.cell.dialog"),
        )
    }
}

@Composable
private fun TableHeader(horizontal: ScrollState, keys: List<String>, widths: Map<String, Dp>) {
    Row(
        Modifier.fillMaxWidth().height(
            40.dp,
        ).background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            "Actions",
            Modifier.width(ACTION_WIDTH).padding(8.dp),
            fontWeight = FontWeight.Bold,
        )
        Row(Modifier.weight(1f).clipToBounds().horizontalScroll(horizontal)) {
            keys.forEach {
                Text(
                    it,
                    Modifier.width(widths.getValue(it)).testTag("runner.header.$it").padding(8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Spacer(
            Modifier.width(reframecv.ui.theme.ReframeTheme.tokens.dimensions.scrollControlSize),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableCells(
    horizontal: ScrollState,
    widths: Map<String, Dp>,
    cells: Map<String, String>,
    open: (String, String) -> Unit,
) {
    Row(Modifier.weight(1f).clipToBounds().horizontalScroll(horizontal)) {
        widths.forEach { (column, width) ->
            val value = cells[column].orEmpty()
            Box(
                Modifier.width(width).fillMaxHeight()
                    .clickable { open(column, value) }.testTag("runner.cell.$column").padding(8.dp),
            ) {
                CellPreview(value, width)
            }
        }
    }
}

@Composable
private fun CellPreview(value: String, width: Dp) {
    val compact = remember(value) { value.replace(Regex("\\s+"), " ").trim() }
    var preview by remember(compact, width) { mutableStateOf(compact) }
    Text(
        preview,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
        onTextLayout = { layout ->
            val last = layout.lineCount - 1
            // Fall back to a literal ellipsis when the text engine only clips maxLines.
            if (last >= 0 && layout.hasVisualOverflow && !layout.isLineEllipsized(last)) {
                val end = layout.getLineEnd(last, visibleEnd = true).coerceAtMost(preview.length)
                val shortened = preview.take((end - 1).coerceAtLeast(0)).trimEnd() + "…"
                if (shortened.length < preview.length) preview = shortened
            }
        },
    )
}

private val ACTION_WIDTH = 120.dp
