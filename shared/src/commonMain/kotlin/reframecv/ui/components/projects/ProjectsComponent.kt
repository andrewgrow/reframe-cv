package reframecv.ui.components.projects

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import reframecv.domain.models.project.Project
import reframecv.ui.components.projects.editor.DefaultEditorComponent
import reframecv.ui.components.projects.editor.EditorComponent
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

interface ProjectsComponent {
    val projectPath: List<ProjectBreadcrumb>
    val uiState: Value<UiState>
    val editorSlot: Value<ChildSlot<*, EditorComponent>>

    fun onAddProject()
    fun onOpenProject(project: Project)
    fun onEditProject(project: Project)
}

class DefaultProjectsComponent(
    componentContext: AppComponentContext,
    storeFactory: StoreFactory = DefaultStoreFactory(),
    override val projectPath: List<ProjectBreadcrumb> = emptyList(),
    private val onProjectOpened: (Project) -> Unit = {},
    executorFactory: () -> ProjectsExecutor = {
        RealProjectsExecutor(
            componentContext.dependencies.projectsRepository,
            projectPath.lastOrNull()?.id,
        )
    },
) : ProjectsComponent,
    AppComponentContext by componentContext {
    private val stateStore = createProjectsStore(storeFactory, executorFactory)
        .also { it.accept(ProjectsIntent.LoadProjects) }
    override val uiState: Value<UiState> = bindStoreToLifecycle(stateStore, lifecycle)

    private sealed interface EditorConfiguration {
        data object Create : EditorConfiguration
        data class Update(val project: Project) : EditorConfiguration
    }

    private val editorNavigation = SlotNavigation<EditorConfiguration>()
    override val editorSlot: Value<ChildSlot<*, EditorComponent>> = childSlot(
        source = editorNavigation,
        serializer = null,
        handleBackButton = true,
    ) { configuration, childContext ->
        val project = (configuration as? EditorConfiguration.Update)?.project
        DefaultEditorComponent(
            childContext,
            onClosed = { editorNavigation.dismiss() },
            initialName = project?.name,
            onDeleted = { project?.let { stateStore.accept(ProjectsIntent.DeleteProject(it.id)) } },
            onSaved = { name ->
                stateStore.accept(
                    if (project == null) {
                        ProjectsIntent.CreateProject(name)
                    } else {
                        ProjectsIntent.UpdateProject(project.id, name)
                    },
                )
            },
        )
    }

    init {
        val subscription = stateStore.labels(
            observer { label ->
                val editor = editorSlot.value.child?.instance as? DefaultEditorComponent
                when (label) {
                    ProjectsLabel.Saving -> editor?.saveState?.value = EditorSaveState.Saving

                    ProjectsLabel.Saved -> editorNavigation.dismiss()

                    ProjectsLabel.SaveFailed -> editor?.saveState?.value = EditorSaveState.Failed

                    ProjectsLabel.Deleting ->
                        editor?.deleteState?.value =
                            EditorDeleteState.Deleting

                    ProjectsLabel.Deleted -> editorNavigation.dismiss()

                    ProjectsLabel.DeleteFailed ->
                        editor?.deleteState?.value =
                            EditorDeleteState.Failed
                }
            },
        )
        lifecycle.doOnDestroy { subscription.dispose() }
    }

    override fun onAddProject() {
        editorNavigation.activate(EditorConfiguration.Create)
    }

    override fun onOpenProject(project: Project) {
        onProjectOpened(project)
    }

    override fun onEditProject(project: Project) {
        editorNavigation.activate(EditorConfiguration.Update(project))
    }
}
