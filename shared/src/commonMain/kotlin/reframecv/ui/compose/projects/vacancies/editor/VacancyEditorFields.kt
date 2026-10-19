package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_company
import reframecv.shared.generated.resources.vacancy_job_details
import reframecv.shared.generated.resources.vacancy_job_title
import reframecv.shared.generated.resources.vacancy_keywords
import reframecv.shared.generated.resources.vacancy_keywords_hint
import reframecv.shared.generated.resources.vacancy_name
import reframecv.shared.generated.resources.vacancy_save_error
import reframecv.shared.generated.resources.vacancy_url
import reframecv.ui.components.common.section.CollapsibleSectionComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.compose.common.CollapsibleSection
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.theme.ReframeTheme

@Suppress("LongParameterList")
@Composable
internal fun VacancyEditorFields(
    draft: VacancyDraft,
    onChange: (VacancyDraft) -> Unit,
    saveState: VacancySaveState,
    onSave: () -> Unit,
    enabled: Boolean,
    nameError: String?,
    details: CollapsibleSectionComponent,
    attachment: @Composable () -> Unit,
) {
    val nameFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()
    var viewport by remember { mutableStateOf(Rect.Zero) }
    val submit = { if (draft.name.isNotBlank() && enabled) onSave() }
    FastScrollColumn(
        scrollState = scroll,
        contentModifier = Modifier.testTag("vacancy.fields")
            .onGloballyPositioned { viewport = it.boundsInWindow() },
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        VacancyField(
            draft.name,
            stringResource(Res.string.vacancy_name),
            enabled,
            submit,
            Modifier.testTag("vacancy.name").focusRequester(nameFocus),
            error = nameError,
        ) {
            onChange(draft.copy(name = it))
        }
        VacancyPostingFields(draft, onChange, enabled, submit)
        VacancyJobDetails(draft, onChange, enabled, scroll, viewport, details)
        VacancyDetailFields(draft, onChange, enabled, submit)
        SelectableText(
            stringResource(Res.string.vacancy_keywords_hint),
            color = ReframeTheme.colorScheme.hint,
            style = ReframeTheme.tokens.typography.bodySmall,
        )
        Spacer(Modifier.height(ReframeTheme.tokens.spacing.large))
        attachment()
        if (saveState == VacancySaveState.Failed) {
            SelectableText(
                stringResource(Res.string.vacancy_save_error),
                color = ReframeTheme.colorScheme.critical,
            )
        }
    }
    LaunchedEffect(Unit) { nameFocus.requestFocus() }
    LaunchedEffect(nameError) {
        if (nameError != null) {
            scroll.animateScrollTo(0)
            nameFocus.requestFocus()
        }
    }
}

@Composable
private fun VacancyPostingFields(
    draft: VacancyDraft,
    onChange: (VacancyDraft) -> Unit,
    enabled: Boolean,
    submit: () -> Unit,
) {
    VacancyField(
        draft.jobTitle,
        stringResource(Res.string.vacancy_job_title),
        enabled,
        submit,
        Modifier.testTag("vacancy.jobTitle"),
    ) { onChange(draft.copy(jobTitle = it)) }
    VacancyField(
        draft.company,
        stringResource(Res.string.vacancy_company),
        enabled,
        submit,
        Modifier.testTag("vacancy.company"),
    ) { onChange(draft.copy(company = it)) }
}

@Composable
private fun VacancyJobDetails(
    draft: VacancyDraft,
    onChange: (VacancyDraft) -> Unit,
    enabled: Boolean,
    scroll: ScrollState,
    viewport: Rect,
    component: CollapsibleSectionComponent,
) {
    val state by component.state.subscribeAsState()
    CollapsibleSection(
        title = stringResource(Res.string.vacancy_job_details),
        state = state,
        toggle = component::onToggle,
        tag = "vacancy.details",
    ) {
        VacancyDescriptionField(
            value = draft.description,
            onValueChange = { onChange(draft.copy(description = it)) },
            enabled = enabled,
            scroll = scroll,
            viewport = viewport,
        )
        VacancyListFields(draft, onChange, enabled, scroll, viewport)
    }
}

@Composable
private fun VacancyDetailFields(
    draft: VacancyDraft,
    onChange: (VacancyDraft) -> Unit,
    enabled: Boolean,
    submit: () -> Unit,
) {
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
    error: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
}
