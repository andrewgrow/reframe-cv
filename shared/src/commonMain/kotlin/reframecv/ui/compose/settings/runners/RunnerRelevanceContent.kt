package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.providers.RunnerResult
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.runner_relevance
import reframecv.shared.generated.resources.runner_relevance_assess
import reframecv.shared.generated.resources.runner_relevance_warning
import reframecv.shared.generated.resources.settings_close
import reframecv.ui.components.settings.runners.RunnerRelevanceComponent
import reframecv.ui.compose.common.AiRequestDialogContent
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.ResizableDialog

@Composable
internal fun RunnerRelevanceCell(
    result: RunnerResult,
    component: RunnerRelevanceComponent,
    enabled: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            { component.assess(result.id) },
            Modifier.testTag("runner.relevance.assess.${result.id}"),
            enabled = enabled,
        ) {
            Icon(Icons.Default.AutoAwesome, stringResource(Res.string.runner_relevance_assess))
        }
        if (result.relevanceScore == null) {
            Text("—", style = MaterialTheme.typography.bodyMedium)
        } else {
            TextButton(
                { component.view(result.id) },
                Modifier.testTag("runner.relevance.score.${result.id}"),
                enabled = enabled,
            ) { Text("${result.relevanceScore}%") }
        }
    }
}

@Composable
internal fun RunnerRelevanceContent(component: RunnerRelevanceComponent) {
    val dialog by component.dialog.subscribeAsState()
    val selection by component.selection.subscribeAsState()
    dialog.component?.let {
        AiRequestDialogContent(
            it,
            stringResource(Res.string.runner_relevance_assess),
            stringResource(Res.string.runner_relevance_warning),
        )
    }
    selection.result?.let { result ->
        ResizableDialog(
            onDismissRequest = component::closeResult,
            title = { Text(stringResource(Res.string.runner_relevance)) },
            text = {
                FastScrollColumn {
                    SelectableText(
                        "${result.relevanceScore}%",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    SelectableText(result.relevancePrompt.orEmpty())
                }
            },
            actions = {
                TextButton(component::closeResult) {
                    Text(stringResource(Res.string.settings_close))
                }
            },
            modifier = Modifier.testTag("runner.relevance.result"),
        )
    }
}
