package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_generate
import reframecv.shared.generated.resources.letter_generate_hint
import reframecv.shared.generated.resources.letter_load_error
import reframecv.shared.generated.resources.letter_missing
import reframecv.shared.generated.resources.letter_open
import reframecv.shared.generated.resources.vacancy_resume_unlink
import reframecv.shared.generated.resources.vacancy_resumes_retry
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyLettersContent(component: VacancyEditorComponent, enabled: Boolean) {
    val draft by component.draft.subscribeAsState()
    val state by component.letters.state.subscribeAsState()
    val resumes by component.resumes.state.subscribeAsState()
    Column(
        Modifier.fillMaxWidth().testTag("vacancy.letters"),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        draft.coverLetterIds.forEach { id ->
            VacancyLetterRow(id, state.available.find { it.id == id }?.name, enabled, component)
        }
        VacancyLetterPicker(
            state.available.filter { it.id !in draft.coverLetterIds },
            enabled && !state.loading,
            component.letters::onSelect,
        )
        if (state.failed) {
            Text(
                stringResource(Res.string.letter_load_error),
                color = ReframeTheme.colorScheme.critical,
            )
            FillButton(
                stringResource(Res.string.vacancy_resumes_retry),
                component.letters::onRetry,
                enabled = enabled,
            )
        }
        val canGenerate = draft.resumes.any { link ->
            resumes.available.any { it.id == link.resumeId }
        }
        FillButton(
            stringResource(Res.string.letter_generate),
            component.letterGeneration::onGenerate,
            Modifier.testTag("vacancy.letters.generate"),
            enabled =
                enabled && canGenerate,
            outlineState = FillButton.OutlineState.Visible,
        )
        if (!canGenerate) {
            Text(
                stringResource(Res.string.letter_generate_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VacancyLetterRow(
    id: Long,
    name: String?,
    enabled: Boolean,
    component: VacancyEditorComponent,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        OutlinedTextField(
            name
                ?: stringResource(Res.string.letter_missing, id),
            {
            },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton({
            component.letters.onOpen(id)
        }, enabled = enabled, modifier = Modifier.testTag("vacancy.letters.open.$id")) {
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                stringResource(Res.string.letter_open),
            )
        }
        FillButton(
            stringResource(Res.string.vacancy_resume_unlink),
            {
                component.letters.onUnlink(id)
            },
            Modifier.testTag(
                "vacancy.letters.unlink.$id",
            ),
            enabled = enabled,
            outlineState = FillButton.OutlineState.Visible,
            fillColor = ReframeTheme.colorScheme.critical,
            contentColor = ReframeTheme.colorScheme.critical,
        )
    }
}
