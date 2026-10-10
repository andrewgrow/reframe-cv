package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_company
import reframecv.shared.generated.resources.vacancy_description
import reframecv.shared.generated.resources.vacancy_keywords
import reframecv.shared.generated.resources.vacancy_keywords_hint
import reframecv.shared.generated.resources.vacancy_save_error
import reframecv.shared.generated.resources.vacancy_title
import reframecv.shared.generated.resources.vacancy_url
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyEditorFields(
    draft: VacancyDraft,
    onChange: (VacancyDraft) -> Unit,
    saveState: VacancySaveState,
    onSave: () -> Unit,
    enabled: Boolean,
) {
    val titleFocus = remember { FocusRequester() }
    val submit = { if (draft.title.isNotBlank() && enabled) onSave() }
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        VacancyField(
            draft.title,
            stringResource(Res.string.vacancy_title),
            enabled,
            submit,
            Modifier.testTag("vacancy.title").focusRequester(titleFocus),
        ) {
            onChange(draft.copy(title = it))
        }
        VacancyField(
            draft.company,
            stringResource(Res.string.vacancy_company),
            enabled,
            submit,
            Modifier.testTag("vacancy.company"),
        ) { onChange(draft.copy(company = it)) }
        VacancyDetailFields(draft, onChange, enabled, submit)
        SelectableText(
            stringResource(Res.string.vacancy_keywords_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
        if (saveState == VacancySaveState.Failed) {
            SelectableText(
                stringResource(Res.string.vacancy_save_error),
                color = ReframeTheme.colorScheme.critical,
            )
        }
    }
    LaunchedEffect(Unit) { titleFocus.requestFocus() }
}

@Composable
private fun VacancyDetailFields(
    draft: VacancyDraft,
    onChange: (VacancyDraft) -> Unit,
    enabled: Boolean,
    submit: () -> Unit,
) {
    OutlinedTextField(
        value = draft.description,
        onValueChange = { onChange(draft.copy(description = it)) },
        modifier = Modifier.fillMaxWidth().testTag("vacancy.description"),
        label = { Text(stringResource(Res.string.vacancy_description)) },
        enabled = enabled,
        minLines = 2,
    )
    VacancyField(
        draft.url,
        stringResource(Res.string.vacancy_url),
        enabled,
        submit,
        Modifier.testTag("vacancy.url"),
    ) { onChange(draft.copy(url = it)) }
    VacancyField(
        draft.keywords,
        stringResource(Res.string.vacancy_keywords),
        enabled,
        submit,
        Modifier.testTag("vacancy.keywords"),
    ) { onChange(draft.copy(keywords = it)) }
}

@Composable
private fun VacancyField(
    value: String,
    label: String,
    enabled: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
}
