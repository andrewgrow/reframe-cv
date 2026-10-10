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
import reframecv.shared.generated.resources.vacancies_empty
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesState
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.projects.vacancies.editor.VacancyEditorContent
import reframecv.ui.theme.ReframeTheme

@Composable
fun VacanciesContent(component: VacanciesComponent) {
    val state by component.uiState.subscribeAsState()
    val editor by component.editorSlot.subscribeAsState()
    val timeZone = remember { TimeZone.currentSystemDefault() }
    key(component.projectId) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                Modifier.fillMaxSize().background(
                    MaterialTheme.colorScheme.background,
                ).safeContentPadding(),
            ) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
                ) {
                    SelectableText(
                        stringResource(Res.string.dashboard_vacancies),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (val current = state) {
                            VacanciesState.Loading -> LoadingContent()

                            VacanciesState.LoadFailed -> VacanciesLoadError()

                            is VacanciesState.Ready -> if (current.vacancies.isEmpty()) {
                                SelectableText(
                                    stringResource(Res.string.vacancies_empty),
                                    color = ReframeTheme.colorScheme.hint,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            } else {
                                VacanciesList(current.vacancies, timeZone, component::onEdit)
                            }
                        }
                    }
                }
                HorizontalDivider()
                VacanciesControls(
                    onAdd = component::onAdd,
                    onBack = component::onBack,
                    onRetry = if (state == VacanciesState.LoadFailed) component::onRetry else null,
                )
            }
            editor.child?.instance?.let { VacancyEditorContent(it) }
        }
    }
}
