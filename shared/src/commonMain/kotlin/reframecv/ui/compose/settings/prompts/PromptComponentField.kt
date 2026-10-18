package reframecv.ui.compose.settings.prompts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.prompts.PromptComponent
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.prompts_all_components
import reframecv.shared.generated.resources.prompts_component
import reframecv.shared.generated.resources.prompts_cover_letters
import reframecv.shared.generated.resources.prompts_projects
import reframecv.shared.generated.resources.prompts_resume
import reframecv.shared.generated.resources.prompts_vacancies

@Composable
internal fun promptComponentLabel(component: PromptComponent?): String = stringResource(
    when (component) {
        null -> Res.string.prompts_all_components
        PromptComponent.Resume -> Res.string.prompts_resume
        PromptComponent.Vacancies -> Res.string.prompts_vacancies
        PromptComponent.CoverLetters -> Res.string.prompts_cover_letters
        PromptComponent.Projects -> Res.string.prompts_projects
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PromptComponentField(
    value: PromptComponent?,
    enabled: Boolean,
    select: (PromptComponent?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            promptComponentLabel(value),
            {},
            Modifier.fillMaxWidth().testTag(
                "prompts.component",
            ).menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            enabled = enabled,
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(Res.string.prompts_component)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            (listOf(null) + PromptComponent.entries).forEach { component ->
                DropdownMenuItem(
                    text = { Text(promptComponentLabel(component)) },
                    onClick = {
                        select(component)
                        expanded = false
                    },
                    modifier = Modifier.testTag("prompts.component.${component?.name ?: "all"}"),
                )
            }
        }
    }
}
