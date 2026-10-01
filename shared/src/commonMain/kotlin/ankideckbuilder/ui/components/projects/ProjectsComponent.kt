package ankideckbuilder.ui.components.projects

import ankideckbuilder.ui.components.projects.editor.DefaultEditorComponent
import ankideckbuilder.ui.components.projects.editor.EditorComponent
import ankideckbuilder.ui.store.bindStoreToLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory

interface ProjectsComponent {
    val uiState: Value<UiState>
    val editorSlot: Value<ChildSlot<*, EditorComponent>>

    fun onAddProject()
}

class DefaultProjectsComponent(
    componentContext: ComponentContext,
    storeFactory: StoreFactory = DefaultStoreFactory(),
    executorFactory: () -> ProjectsExecutor = ::RealProjectsExecutor,
) : ProjectsComponent,
    ComponentContext by componentContext {
    private val stateStore = createProjectsStore(storeFactory, executorFactory)
        .also { it.accept(ProjectsIntent.LoadProjects) }
    override val uiState: Value<UiState> = bindStoreToLifecycle(stateStore, lifecycle)

    private val editorNavigation = SlotNavigation<Unit>()
    override val editorSlot: Value<ChildSlot<*, EditorComponent>> = childSlot(
        source = editorNavigation,
        serializer = null,
        handleBackButton = true,
    ) { _, childContext ->
        DefaultEditorComponent(childContext, onClosed = { editorNavigation.dismiss() })
    }

    override fun onAddProject() {
        editorNavigation.activate(Unit)
    }
}
