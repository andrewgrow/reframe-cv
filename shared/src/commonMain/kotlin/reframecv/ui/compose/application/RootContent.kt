package reframecv.ui.compose.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import reframecv.ui.components.application.RootComponent
import reframecv.ui.components.projects.resumes.ResumesComponent
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.compose.common.LocalDocumentFooterHeight
import reframecv.ui.compose.common.documentFooterMinHeight
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.compose.projects.ProjectsContent
import reframecv.ui.compose.projects.dashboard.DashboardContent
import reframecv.ui.compose.projects.resumes.ResumesContent
import reframecv.ui.compose.projects.vacancies.VacanciesContent
import reframecv.ui.compose.settings.SettingsContent

@Composable
fun RootContent(component: RootComponent) {
    val balance by component.balanceNotice.subscribeAsState()
    val navigationState by component.navigationPanel.state.subscribeAsState()
    if (navigationState.loading) return
    val stack by component.childStack.subscribeAsState()
    val active = stack.active.instance
    val editorOpen = projectDialogOpen(active)
    val documentEditor = hasDocumentEditor(active)
    val measuredFooter = remember(active, documentEditor) { mutableStateOf<Dp?>(null) }
    val reportFooter = remember(measuredFooter) { { height: Dp? -> measuredFooter.value = height } }
    val footerHeight = if (documentEditor) {
        maxOf(measuredFooter.value ?: documentFooterMinHeight(), documentFooterMinHeight())
    } else {
        null
    }
    CompositionLocalProvider(LocalDocumentFooterHeight provides reportFooter) {
        Row(
            Modifier.testTag(
                "app.root",
            ).fillMaxSize().background(MaterialTheme.colorScheme.background).pageBackHandler(
                enabled = (
                    active !is RootComponent.Child.Projects ||
                        active.component.projectPath.isNotEmpty()
                    ) && !editorOpen,
                destination = active,
                onBack = {
                    when (active) {
                        is RootComponent.Child.Settings -> active.component.onBack()
                        is RootComponent.Child.Resumes -> active.component.onBack()
                        is RootComponent.Child.Dashboard -> active.component.onBack()
                        is RootComponent.Child.Vacancies -> active.component.onBack()
                        is RootComponent.Child.Projects -> active.component.onBack()
                    }
                },
            ),
        ) {
            RootNavigation(
                component.projectTree,
                panel = component.navigationPanel,
                onSettings = component::onSettings,
                settingsSelected = active is RootComponent.Child.Settings,
                footerHeight = footerHeight,
                balance = balance.balance,
            )
            VerticalDivider()
            Children(
                stack = component.childStack,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) { child ->
                RootChildContent(child.instance)
            }
        }
    }
}

@Composable
private fun RootChildContent(child: RootComponent.Child) {
    when (val instance = child) {
        is RootComponent.Child.Settings -> SettingsContent(instance.component)
        is RootComponent.Child.Projects -> ProjectsContent(instance.component)
        is RootComponent.Child.Resumes -> ResumesContent(instance.component)
        is RootComponent.Child.Dashboard -> DashboardContent(instance.component)
        is RootComponent.Child.Vacancies -> VacanciesContent(instance.component)
    }
}

@Composable
private fun hasDocumentEditor(child: RootComponent.Child): Boolean = when (child) {
    is RootComponent.Child.Resumes -> {
        val stack by child.component.childStack.subscribeAsState()
        stack.active.instance is ResumesComponent.Child.Editor
    }

    is RootComponent.Child.Vacancies -> {
        val stack by child.component.childStack.subscribeAsState()
        stack.active.instance is VacanciesComponent.Child.Editor
    }

    else -> false
}

@Composable
private fun projectDialogOpen(child: RootComponent.Child): Boolean {
    if (child !is RootComponent.Child.Projects) return false
    val editor by child.component.editorSlot.subscribeAsState()
    return editor.child != null
}
