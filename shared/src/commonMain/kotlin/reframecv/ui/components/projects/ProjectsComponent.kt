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
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

interface ProjectsComponent {
    val uiState: Value<UiState>
    val editorSlot: Value<ChildSlot<*, EditorComponent>>

    fun onAddProject()
    fun onProjectClick(project: Project)
}

class DefaultProjectsComponent(
    componentContext: AppComponentContext,
    storeFactory: StoreFactory = DefaultStoreFactory(),
    executorFactory: () -> ProjectsExecutor = {
        RealProjectsExecutor(componentContext.dependencies.projectsRepository)
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
                }
            },
        )
        lifecycle.doOnDestroy { subscription.dispose() }
    }

    override fun onAddProject() {
        editorNavigation.activate(EditorConfiguration.Create)
    }

    override fun onProjectClick(project: Project) {
        editorNavigation.activate(EditorConfiguration.Update(project))
    }
}
