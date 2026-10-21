package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_choose
import reframecv.shared.generated.resources.letter_empty
import reframecv.shared.generated.resources.letter_search
import reframecv.shared.generated.resources.letter_title
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VacancyLetterPicker(
    choices: List<CoverLetter>,
    enabled: Boolean,
    select: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    ExposedDropdownMenuBox(expanded && enabled, {
        if (enabled) {
            expanded = it
            if (it) query = ""
        }
    }, modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(Res.string.letter_title)) },
            placeholder = {
                Text(
                    stringResource(
                        if (expanded) {
                            Res.string.letter_search
                        } else {
                            Res.string.letter_choose
                        },
                    ),
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            modifier = Modifier.fillMaxWidth().testTag("vacancy.letters.select")
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            VacancyLetterOptions(choices, query) {
                select(it)
                query = ""
                expanded = false
            }
        }
    }
}

@Composable
private fun VacancyLetterOptions(
    choices: List<CoverLetter>,
    query: String,
    select: (Long) -> Unit,
) {
    val scroll = rememberScrollState()
    val tokens = ReframeTheme.tokens
    val search = query.trim()
    val matches = choices.filter { resume ->
        resume.name.contains(search, ignoreCase = true) ||
            resume.keywords.any { it.contains(search, ignoreCase = true) }
    }
    LaunchedEffect(search) { scroll.scrollTo(0) }
    FastScrollColumn(
        modifier = Modifier.height(tokens.dimensions.providerModelMenuMaxHeight)
            .padding(horizontal = tokens.spacing.small).testTag("vacancy.letters.menu"),
        scrollState = scroll,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        if (matches.isEmpty()) Text(stringResource(Res.string.letter_empty))
        matches.forEach { resume ->
            DropdownMenuItem(
                text = { Text(resume.name) },
                onClick = { select(resume.id) },
                modifier = Modifier.padding(vertical = tokens.spacing.small)
                    .testTag("vacancy.letters.option.${resume.id}"),
            )
        }
    }
}
