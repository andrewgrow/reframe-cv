package reframecv.ui.components.projects

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
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

/** Lets the project screen browse children and manage projects through a shared editor. */
interface ProjectsComponent {
    val projectPath: List<ProjectPathEntry>
    val uiState: Value<UiState>
    val editorSlot: Value<ChildSlot<*, EditorComponent>>

    fun onBack()
    fun onConfigureProject()
    fun onAddProject()
    fun onOpenProject(project: Project)
    fun onEditProject(project: Project)
}

/** Keeps project browsing, editing, and Back behavior working together. */
class DefaultProjectsComponent(
    componentContext: AppComponentContext,
    storeFactory: StoreFactory = DefaultStoreFactory(),
    override val projectPath: List<ProjectPathEntry> = emptyList(),
    private val onProjectOpened: (Project) -> Unit = {},
    private val onProjectConfigured: () -> Unit = {},
    private val back: () -> Unit = {},
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

    /** Selects project creation or editing. */
    private sealed interface EditorConfiguration {
        /** Opens creation in the current project list. */
        data object Create : EditorConfiguration

        /** Opens editing for an existing project. */
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
        val projectBack = BackCallback(isEnabled = projectPath.isNotEmpty()) { onBack() }
        backHandler.register(projectBack)
        val editorSubscription = editorSlot.subscribe {
            projectBack.isEnabled = projectPath.isNotEmpty() && it.child == null
        }
        lifecycle.doOnDestroy {
            editorSubscription.cancel()
            backHandler.unregister(projectBack)
        }
        val subscription = stateStore.labels(
            observer { label ->
                val editor = editorSlot.value.child?.instance as? DefaultEditorComponent
                when (label) {
                    ProjectsLabel.Saving -> editor?.saveState?.value = EditorSaveState.Saving

                    is ProjectsLabel.Created -> {
                        editorNavigation.dismiss()
                        onProjectOpened(label.project)
                    }

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

    override fun onBack() = back()

    override fun onConfigureProject() {
        if (projectPath.isNotEmpty()) onProjectConfigured()
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
