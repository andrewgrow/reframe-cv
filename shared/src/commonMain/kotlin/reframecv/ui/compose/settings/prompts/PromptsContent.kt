package reframecv.ui.compose.settings.prompts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.prompts.Prompt
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.prompts_add
import reframecv.shared.generated.resources.prompts_empty
import reframecv.shared.generated.resources.prompts_error_load
import reframecv.shared.generated.resources.prompts_list_component
import reframecv.shared.generated.resources.prompts_list_title
import reframecv.shared.generated.resources.prompts_title
import reframecv.shared.generated.resources.prompts_untitled
import reframecv.shared.generated.resources.settings_active
import reframecv.shared.generated.resources.settings_disabled
import reframecv.shared.generated.resources.settings_status
import reframecv.ui.components.settings.prompts.PromptsComponent
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun PromptsContent(component: PromptsComponent) {
    val editor by component.editorSlot.subscribeAsState()
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        editor.child?.instance?.let { PromptEditorContent(it) } ?: PromptsListContent(component)
    }
}

@Composable
private fun PromptsListContent(component: PromptsComponent) {
    val state by component.state.subscribeAsState()
    val spacing = ReframeTheme.tokens.spacing
    Column(
        Modifier.fillMaxSize().background(
            MaterialTheme.colorScheme.background,
        ).testTag("prompts.page"),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            PageHeader(stringResource(Res.string.prompts_title), component::onBack)
            if (state.loading) {
                LoadingContent()
            } else {
                FastScrollColumn {
                    if (state.failed) {
                        Text(
                            stringResource(Res.string.prompts_error_load),
                            color = ReframeTheme.colorScheme.critical,
                        )
                    }
                    if (!state.failed &&
                        state.prompts.isEmpty()
                    ) {
                        Text(stringResource(Res.string.prompts_empty))
                    }
                    if (state.prompts.isNotEmpty()) PromptTableHeader()
                    state.prompts.forEach { PromptRow(it) { component.onEdit(it) } }
                }
            }
        }
        HorizontalDivider()
        PromptsActions(component, state.failed)
    }
}

@Composable
private fun PromptRow(prompt: Prompt, open: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(
            onClick = open,
        ).padding(ReframeTheme.tokens.spacing.medium).testTag("prompts.row.${prompt.id}"),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Text(
            prompt.title.ifBlank {
                stringResource(Res.string.prompts_untitled, prompt.id)
            },
            Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            promptComponentLabel(prompt.component),
            Modifier.weight(COMPONENT_COLUMN_WEIGHT),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(
                if (prompt.active) Res.string.settings_active else Res.string.settings_disabled,
            ),
            Modifier.weight(1f),
            maxLines = 1,
        )
    }
    HorizontalDivider()
}

@Composable
private fun PromptsActions(component: PromptsComponent, failed: Boolean) {
    val spacing = ReframeTheme.tokens.spacing
    Row(
        Modifier.fillMaxWidth().padding(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(
            spacing.medium,
            androidx.compose.ui.Alignment.CenterHorizontally,
        ),
    ) {
        FillButton(
            stringResource(Res.string.prompts_add),
            component::onAdd,
            Modifier.testTag("prompts.add"),
            outlineState = FillButton.OutlineState.Visible,
        )
        if (failed) {
            FillButton(
                stringResource(Res.string.navigation_retry),
                component::onRetry,
            )
        }
    }
}

@Composable
private fun PromptTableHeader() {
    Row(
        Modifier.fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Text(
            stringResource(Res.string.prompts_list_title),
            Modifier.weight(2f),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(Res.string.prompts_list_component),
            Modifier.weight(COMPONENT_COLUMN_WEIGHT),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(Res.string.settings_status),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
    }
    HorizontalDivider()
}

private const val COMPONENT_COLUMN_WEIGHT = 1.5f
