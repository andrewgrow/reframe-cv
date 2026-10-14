package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_add_entry
import reframecv.shared.generated.resources.resume_remove
import reframecv.ui.theme.ReframeTheme

/** Keeps repeated section controls and form fields consistent across the structured editor. */
internal class ResumeFormScope(val enabled: Boolean) {
    @Composable
    fun Field(
        value: String,
        label: StringResource,
        onChange: (String) -> Unit,
        multiline: Boolean = false,
        tag: String = "",
        error: String? = null,
        minLines: Int = if (multiline) DEFAULT_MULTILINE_MIN_LINES else 1,
        placeholder: StringResource? = null,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().testTag(tag),
            label = { Text(stringResource(label)) },
            placeholder = placeholder?.let { hint -> { Text(stringResource(hint)) } },
            singleLine = !multiline,
            minLines = minLines,
            enabled = enabled,
            isError = error != null,
            supportingText = error?.let { message -> { Text(message) } },
        )
    }

    @Composable
    fun Action(label: StringResource, onClick: () -> Unit, allowed: Boolean = true) {
        TextButton(onClick, enabled = enabled && allowed) { Text(stringResource(label)) }
    }

    @Composable
    fun Toggle(value: Boolean, label: StringResource, onChange: (Boolean) -> Unit) {
        Row {
            Checkbox(value, onCheckedChange = onChange, enabled = enabled)
            TextButton({ onChange(!value) }, enabled = enabled) { Text(stringResource(label)) }
        }
    }

    @Composable
    fun Heading(label: StringResource) {
        SelectableText(stringResource(label), style = ReframeTheme.tokens.typography.titleMedium)
    }

    @Composable
    fun <T> Entries(
        items: List<T>,
        onChange: (List<T>) -> Unit,
        create: () -> T,
        addLabel: StringResource = Res.string.resume_add_entry,
        removeAction: @Composable (T, () -> Unit) -> Unit = { _, remove ->
            FlowRow { Action(Res.string.resume_remove, remove) }
        },
        fields: @Composable ResumeFormScope.(T, (T) -> Unit) -> Unit,
    ) {
        items.forEachIndexed { index, item ->
            key(index, items.size) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
                ) {
                    fields(item) { replacement -> onChange(items.replacedAt(index, replacement)) }
                    removeAction(item) { onChange(items.removedAt(index)) }
                }
            }
        }
        Action(addLabel, { onChange(items + create()) })
    }
}

internal fun <T> List<T>.replacedAt(index: Int, replacement: T): List<T> =
    mapIndexed { i, value -> if (i == index) replacement else value }

internal fun <T> List<T>.removedAt(index: Int): List<T> = filterIndexed { i, _ -> i != index }

private const val DEFAULT_MULTILINE_MIN_LINES = 3
