package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_resize
import reframecv.shared.generated.resources.resume_keywords
import reframecv.shared.generated.resources.resume_name
import reframecv.shared.generated.resources.resume_text
import reframecv.ui.components.projects.coverletters.CoverLetterEditorComponent
import reframecv.ui.components.projects.coverletters.LetterNotice
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.ResizableTextField
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun CoverLetterFields(component: CoverLetterEditorComponent, modifier: Modifier) {
    val state by component.state.subscribeAsState()
    val tokens = ReframeTheme.tokens
    val scroll = rememberScrollState()
    var viewport by remember { mutableStateOf(Rect.Zero) }
    FastScrollColumn(
        modifier.padding(tokens.spacing.medium).onGloballyPositioned {
            viewport =
                it.boundsInWindow()
        },
        scrollState = scroll,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        CoverLetterImportFeedback(component.importComponent)
        CoverLetterIdentity(component)
        ResizableTextField(
            state.record.content, { component.onChange(state.record.copy(content = it)) },
            !state.busy && !state.confirming && !state.exportVisible, scroll, viewport,
            stringResource(Res.string.resume_text), stringResource(Res.string.letter_resize),
            "cover_letter.text", tokens.dimensions.resumeTextMinHeight,
            initialHeight = tokens.dimensions.resumeTextMinHeight * 3,
        )
    }
}

@Composable
private fun CoverLetterIdentity(component: CoverLetterEditorComponent) {
    val state by component.state.subscribeAsState()
    val enabled = !state.busy && !state.confirming && !state.exportVisible
    OutlinedTextField(
        state.record.name,
        { component.onChange(state.record.copy(name = it)) },
        enabled = enabled,
        isError =
            state.notice == LetterNotice.Validation && state.record.name.isBlank(),
        label = { Text(stringResource(Res.string.resume_name)) },
        modifier = Modifier.fillMaxWidth().testTag("cover_letter.name"),
        singleLine = true,
    )
    OutlinedTextField(
        state.record.keywords.joinToString(", "),
        {
            component.onChange(state.record.copy(keywords = it.split(',').map(String::trim)))
        },
        enabled = enabled,
        label = { Text(stringResource(Res.string.resume_keywords)) },
        modifier = Modifier.fillMaxWidth().testTag("cover_letter.keywords"),
    )
}
