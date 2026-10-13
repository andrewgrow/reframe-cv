package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.ui.components.projects.resumes.editor.DefaultResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

/** Lets a project browse its resumes and create structured drafts. */
interface ResumesComponent {
    val projectId: Long
    val uiState: Value<ResumesState>
    val childStack: Value<ChildStack<*, Child>>
    val editor: ResumeEditorComponent?
        get() = (childStack.value.active.instance as? Child.Editor)?.component

    /** Identifies the resume list or its full-page editor. */
    sealed interface Child {
        /** Displays the current project's resume list. */
        data object List : Child

        /** Displays resume creation within the application shell. */
        data class Editor(val component: ResumeEditorComponent) : Child
    }
    fun onAdd()
    fun onRetry()
    fun onBack()
}

/** Keeps resume browsing and editing together without Back unexpectedly leaving the page. */
class DefaultResumesComponent(
    componentContext: AppComponentContext,
    override val projectId: Long,
    private val back: () -> Unit,
    storeFactory: StoreFactory = componentContext.dependencies.storeFactory,
) : ResumesComponent,
    AppComponentContext by componentContext {
    private val store = createResumesStore(
        storeFactory,
        projectId,
        dependencies.resumesRepository,
        dependencies.logger,
    )
        .also { it.accept(ResumesIntent.Load) }
    override val uiState = bindStoreToLifecycle(store, lifecycle)

    /** Selects the resume list or creation page. */
    private sealed interface EditorConfiguration {
        /** Opens the resume list. */
        data object List : EditorConfiguration

        /** Opens creation of a new resume. */
        data object Create : EditorConfiguration
    }

    private val editorNavigation = StackNavigation<EditorConfiguration>()
    override val childStack: Value<ChildStack<*, ResumesComponent.Child>> = childStack(
        source = editorNavigation,
        serializer = null,
        initialConfiguration = EditorConfiguration.List,
        handleBackButton = false,
    ) { configuration, childContext ->
        if (configuration == EditorConfiguration.List) {
            ResumesComponent.Child.List
        } else {
            ResumesComponent.Child.Editor(
                DefaultResumeEditorComponent(
                    childContext,
                    save = { draft -> store.accept(ResumesIntent.Create(draft)) },
                    close = { editorNavigation.pop() },
                ),
            )
        }
    }

    init {
        val editorBack = BackCallback(isEnabled = false) { onBack() }
        backHandler.register(editorBack)
        val stackSubscription = childStack.subscribe {
            editorBack.isEnabled = it.active.instance is ResumesComponent.Child.Editor
        }
        lifecycle.doOnDestroy {
            stackSubscription.cancel()
            backHandler.unregister(editorBack)
        }
        val subscription = store.labels(
            observer { label ->
                val editor = editor as? DefaultResumeEditorComponent
                when (label) {
                    ResumesLabel.Saving -> editor?.saveState?.value = ResumeSaveState.Saving
                    ResumesLabel.Saved -> editorNavigation.pop()
                    ResumesLabel.SaveFailed -> editor?.saveState?.value = ResumeSaveState.Failed
                }
            },
        )
        lifecycle.doOnDestroy { subscription.dispose() }
    }

    override fun onAdd() {
        if (editor == null) editorNavigation.pushNew(EditorConfiguration.Create)
    }

    override fun onRetry() = store.accept(ResumesIntent.Load)
    override fun onBack() {
        val editor = editor
        if (editor != null) editor.onClose() else back()
    }
}
