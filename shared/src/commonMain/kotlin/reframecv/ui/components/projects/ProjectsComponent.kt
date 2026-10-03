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
import reframecv.ui.components.projects.editor.DefaultEditorComponent
import reframecv.ui.components.projects.editor.EditorComponent
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

interface ProjectsComponent {
    val uiState: Value<UiState>
    val editorSlot: Value<ChildSlot<*, EditorComponent>>

    fun onAddProject()
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

    private val editorNavigation = SlotNavigation<Unit>()
    override val editorSlot: Value<ChildSlot<*, EditorComponent>> = childSlot(
        source = editorNavigation,
        serializer = null,
        handleBackButton = true,
    ) { _, childContext ->
        DefaultEditorComponent(
            childContext,
            onClosed = { editorNavigation.dismiss() },
            onSaved = { stateStore.accept(ProjectsIntent.CreateProject(it)) },
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
        editorNavigation.activate(Unit)
    }
}
