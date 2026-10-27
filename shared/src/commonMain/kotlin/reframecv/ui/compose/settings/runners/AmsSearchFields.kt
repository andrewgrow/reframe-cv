package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.runner_ams_any_time
import reframecv.shared.generated.resources.runner_ams_external
import reframecv.shared.generated.resources.runner_ams_hint
import reframecv.shared.generated.resources.runner_ams_location
import reframecv.shared.generated.resources.runner_ams_month
import reframecv.shared.generated.resources.runner_ams_period
import reframecv.shared.generated.resources.runner_ams_query
import reframecv.shared.generated.resources.runner_ams_radius
import reframecv.shared.generated.resources.runner_ams_week
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun AmsSearchFields(
    draft: RunnerDraft,
    onDraft: (RunnerDraft) -> Unit,
    enabled: Boolean,
    validation: Boolean,
    tagPrefix: String,
) {
    Text(stringResource(Res.string.runner_ams_hint), color = ReframeTheme.colorScheme.hint)
    OutlinedTextField(
        draft.query,
        { onDraft(draft.copy(query = it)) },
        Modifier.fillMaxWidth().testTag("$tagPrefix.query"),
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(Res.string.runner_ams_query)) },
        isError = validation && draft.query.isBlank(),
    )
    OutlinedTextField(
        draft.location,
        { onDraft(draft.copy(location = it)) },
        Modifier.fillMaxWidth().testTag("$tagPrefix.location"),
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(Res.string.runner_ams_location)) },
        isError = validation && draft.radiusKm.trim() != "0" && draft.location.isBlank(),
    )
    AmsSearchOptions(draft, onDraft, enabled, tagPrefix)
}

@Composable
private fun AmsSearchOptions(
    draft: RunnerDraft,
    onDraft: (RunnerDraft) -> Unit,
    enabled: Boolean,
    tagPrefix: String,
) {
    val radii = listOf("0", "5", "10", "20", "50", "75", "100")
    RunnerSelectField(
        draft.radiusKm,
        radii,
        stringResource(Res.string.runner_ams_radius),
        "$tagPrefix.radius",
        enabled,
    ) { onDraft(draft.copy(radiusKm = radii[it])) }
    val periods = listOf("", "LAST_WEEK", "LAST_MONTH")
    val labels = listOf(
        Res.string.runner_ams_any_time,
        Res.string.runner_ams_week,
        Res.string.runner_ams_month,
    ).map { stringResource(it) }
    RunnerSelectField(
        labels[periods.indexOf(draft.postedWithin).coerceAtLeast(0)],
        labels,
        stringResource(Res.string.runner_ams_period),
        "$tagPrefix.period",
        enabled,
    ) {
        onDraft(draft.copy(postedWithin = periods[it]))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            draft.includeExternalJobs,
            { onDraft(draft.copy(includeExternalJobs = it)) },
            enabled = enabled,
            modifier = Modifier.testTag("$tagPrefix.external"),
        )
        Text(stringResource(Res.string.runner_ams_external))
    }
}
