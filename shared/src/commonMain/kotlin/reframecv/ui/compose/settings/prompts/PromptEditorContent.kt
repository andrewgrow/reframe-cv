package reframecv.ui.compose.settings.prompts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.prompts_editor
import reframecv.shared.generated.resources.prompts_new
import reframecv.shared.generated.resources.prompts_optional_title
import reframecv.shared.generated.resources.prompts_text
import reframecv.shared.generated.resources.settings_close
import reframecv.shared.generated.resources.settings_save
import reframecv.ui.components.settings.prompts.PromptEditorComponent
import reframecv.ui.components.settings.prompts.PromptEditorError
import reframecv.ui.components.settings.prompts.PromptEditorState
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.FillButton
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.settings.ProviderStatusField
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun PromptEditorContent(component: PromptEditorComponent) {
    val state by component.state.subscribeAsState()
    val spacing = ReframeTheme.tokens.spacing
    Column(
        Modifier.fillMaxSize().background(
            MaterialTheme.colorScheme.background,
        ).testTag("prompts.editor"),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            PageHeader(
                stringResource(
                    if (state.prompt.id ==
                        0L
                    ) {
                        Res.string.prompts_new
                    } else {
                        Res.string.prompts_editor
                    },
                ),
                component::onBack,
                !state.saving,
            )
            PromptFields(state, component)
        }
        HorizontalDivider()
        PromptSaveFeedback(state)
        PromptActions(state, component)
    }
}

@Composable
private fun PromptFields(state: PromptEditorState, component: PromptEditorComponent) {
    val spacing = ReframeTheme.tokens.spacing
    FastScrollColumn(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        ProviderStatusField(
            state.prompt.active,
            !state.saving,
            component::onActive,
            Modifier.testTag("prompts.status"),
        )
        OutlinedTextField(
            state.prompt.title,
            component::onTitle,
            Modifier.fillMaxWidth().testTag("prompts.title"),
            enabled = !state.saving,
            singleLine = true,
            label = { Text(stringResource(Res.string.prompts_optional_title)) },
        )
        PromptComponentField(state.prompt.component, !state.saving, component::onComponent)
        OutlinedTextField(
            state.prompt.text,
            component::onText,
            Modifier.fillMaxWidth().testTag("prompts.text"),
            enabled = !state.saving,
            minLines = 5,
            isError =
                state.error == PromptEditorError.Validation && state.prompt.text.isBlank(),
            label = { Text(stringResource(Res.string.prompts_text)) },
        )
    }
}

@Composable
private fun PromptActions(state: PromptEditorState, component: PromptEditorComponent) {
    val spacing = ReframeTheme.tokens.spacing
    Row(
        Modifier.fillMaxWidth().padding(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(
            spacing.medium,
            androidx.compose.ui.Alignment.CenterHorizontally,
        ),
    ) {
        FillButton(
            stringResource(Res.string.settings_save),
            component::onSave,
            Modifier.testTag("prompts.save"),
            enabled = !state.saving,
            outlineState = FillButton.OutlineState.Visible,
        )
        FillButton(
            stringResource(Res.string.settings_close),
            component::onBack,
            enabled = !state.saving,
        )
    }
}
