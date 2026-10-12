package reframecv.ui.components.projects.vacancies

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
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

/** Lets the vacancy screen browse opportunities and manage them through a shared editor. */
interface VacanciesComponent {
    val projectId: Long
    val uiState: Value<VacanciesState>
    val childStack: Value<ChildStack<*, Child>>
    val editor: VacancyEditorComponent?
        get() = (childStack.value.active.instance as? Child.Editor)?.component

    /** Identifies the vacancy list or its full-page editor. */
    sealed interface Child {
        /** Displays the current project's vacancy list. */
        data object List : Child

        /** Displays creation or editing without covering the application shell. */
        data class Editor(val component: VacancyEditorComponent) : Child
    }
    fun onEdit(vacancy: Vacancy)
    fun onAdd()
    fun onRetry()
    fun onBack()
}

/** Keeps vacancy browsing and editing together without Back unexpectedly leaving the page. */
class DefaultVacanciesComponent(
    componentContext: AppComponentContext,
    override val projectId: Long,
    private val back: () -> Unit,
    storeFactory: StoreFactory = componentContext.dependencies.storeFactory,
) : VacanciesComponent,
    AppComponentContext by componentContext {
    private val store = createVacanciesStore(
        storeFactory,
        projectId,
        dependencies.vacanciesRepository,
        dependencies.logger,
    )
        .also { it.accept(VacanciesIntent.Load) }
    override val uiState = bindStoreToLifecycle(store, lifecycle)

    /** Selects vacancy creation or editing. */
    private sealed interface EditorConfiguration {
        /** Opens the vacancy list. */
        data object List : EditorConfiguration

        /** Opens creation of a new vacancy. */
        data object Create : EditorConfiguration

        /** Opens editing for an existing vacancy. */
        data class Edit(val vacancy: Vacancy) : EditorConfiguration
    }

    private val editorNavigation = StackNavigation<EditorConfiguration>()
    override val childStack: Value<ChildStack<*, VacanciesComponent.Child>> = childStack(
        source = editorNavigation,
        serializer = null,
        initialConfiguration = EditorConfiguration.List,
        handleBackButton = false,
    ) { configuration, childContext ->
        if (configuration == EditorConfiguration.List) {
            VacanciesComponent.Child.List
        } else {
            val original = (configuration as? EditorConfiguration.Edit)?.vacancy
            VacanciesComponent.Child.Editor(
                DefaultVacancyEditorComponent(
                    childContext,
                    save = { draft ->
                        store.accept(
                            if (original == null) {
                                VacanciesIntent.Create(draft)
                            } else {
                                VacanciesIntent.Update(original, draft)
                            },
                        )
                    },
                    initialVacancy = original,
                    delete = { original?.let { store.accept(VacanciesIntent.Delete(it.id)) } },
                    close = { editorNavigation.pop() },
                ),
            )
        }
    }

    init {
        val editorBack = BackCallback(isEnabled = false) { onBack() }
        backHandler.register(editorBack)
        val stackSubscription = childStack.subscribe {
            editorBack.isEnabled = it.active.instance is VacanciesComponent.Child.Editor
        }
        lifecycle.doOnDestroy {
            stackSubscription.cancel()
            backHandler.unregister(editorBack)
        }
        val subscription = store.labels(
            observer { label ->
                val editor = editor as? DefaultVacancyEditorComponent
                when (label) {
                    VacanciesLabel.Saving -> editor?.saveState?.value = VacancySaveState.Saving

                    VacanciesLabel.Saved, VacanciesLabel.Deleted -> editorNavigation.pop()

                    VacanciesLabel.Deleting ->
                        editor?.deleteState?.value =
                            VacancyDeleteState.Deleting

                    VacanciesLabel.DeleteFailed ->
                        editor?.deleteState?.value =
                            VacancyDeleteState.Failed

                    VacanciesLabel.SaveFailed -> editor?.saveState?.value = VacancySaveState.Failed
                }
            },
        )
        lifecycle.doOnDestroy { subscription.dispose() }
    }

    override fun onAdd() {
        if (editor == null) editorNavigation.pushNew(EditorConfiguration.Create)
    }

    override fun onEdit(vacancy: Vacancy) {
        if (editor == null && vacancy.projectId == projectId) {
            editorNavigation.pushNew(EditorConfiguration.Edit(vacancy))
        }
    }

    override fun onRetry() = store.accept(VacanciesIntent.Load)
    override fun onBack() {
        val editor = editor
        if (editor != null) editor.onClose() else back()
    }
}
