package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.dashboard_vacancies
import reframecv.shared.generated.resources.vacancies_applied_error
import reframecv.shared.generated.resources.vacancies_empty
import reframecv.shared.generated.resources.vacancies_filter_empty
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesState
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacanciesListContent(component: VacanciesComponent) {
    val state by component.uiState.subscribeAsState()
    val selection by component.selection.state.subscribeAsState()
    val timeZone = remember { TimeZone.currentSystemDefault() }
    key(component.projectId) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                Modifier.fillMaxSize().background(
                    MaterialTheme.colorScheme.background,
                ).safeContentPadding().pageBackHandler(
                    component::onBack,
                    enabled = !selection.confirming,
                ),
            ) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
                ) {
                    PageHeader(
                        stringResource(Res.string.dashboard_vacancies),
                        component::onBack,
                        enabled = !selection.busy,
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (val current = state) {
                            VacanciesState.Loading -> LoadingContent()

                            VacanciesState.LoadFailed -> VacanciesLoadError()

                            is VacanciesState.Ready -> if (selection.visible(
                                    current.vacancies,
                                ).isEmpty()
                            ) {
                                SelectableText(
                                    stringResource(
                                        if (current.vacancies.isEmpty()) {
                                            Res.string.vacancies_empty
                                        } else {
                                            Res.string.vacancies_filter_empty
                                        },
                                    ),
                                    color = ReframeTheme.colorScheme.hint,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            } else {
                                VacanciesList(
                                    selection.visible(current.vacancies),
                                    timeZone,
                                    component::onEdit,
                                    component.selection,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
                VacanciesListControls(component)
            }
            VacancySelectionConfirmation(component)
        }
    }
}

@Composable
private fun VacanciesListControls(component: VacanciesComponent) {
    val state by component.uiState.subscribeAsState()
    val selection by component.selection.state.subscribeAsState()
    if (selection.applicationFailed) {
        SelectableText(
            stringResource(Res.string.vacancies_applied_error),
            color = ReframeTheme.colorScheme.critical,
            selectionModifier = Modifier.padding(ReframeTheme.tokens.spacing.medium),
        )
    }
    VacanciesControls(
        onApplied = if (state is VacanciesState.Ready) component.selection::onMarkApplied else null,
        onAdd = component::onAdd,
        onDelete = if (state is VacanciesState.Ready) {
            component.selection::onRequestDelete
        } else {
            null
        },
        selectedCount = selection.selected.size,
        hideApplied = selection.hideApplied,
        onToggleAppliedFilter = if (state is VacanciesState.Ready) {
            component.selection::onToggleAppliedFilter
        } else {
            null
        },
        enabled = !selection.confirming && !selection.busy,
        onRetry = if (state == VacanciesState.LoadFailed) component::onRetry else null,
    )
}
