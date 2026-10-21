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
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.coverletters.CoverLetterEditorComponent
import reframecv.ui.components.projects.coverletters.DefaultCoverLetterEditorComponent
import reframecv.ui.components.projects.resumes.DefaultResumesComponent
import reframecv.ui.components.projects.resumes.ResumesComponent
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

/** Lets the vacancy screen browse opportunities and manage them through a shared editor. */
interface VacanciesComponent {
    val selection: VacancySelectionComponent
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

        /** Opens a linked resume while retaining the vacancy draft underneath. */
        data class LinkedResume(val component: ResumesComponent) : Child

        /** Displays an attached letter without discarding the vacancy draft. */
        data class LinkedLetter(val component: CoverLetterEditorComponent) : Child
    }
    val isBusy: Boolean get() = when (val child = childStack.value.active.instance) {
        is Child.Editor -> child.component.isBusy
        is Child.LinkedResume -> child.component.isBusy
        is Child.LinkedLetter -> child.component.state.value.busy
        Child.List -> selection.state.value.busy
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
    initialVacancyId: Long? = null,
) : VacanciesComponent,
    AppComponentContext by componentContext {
    private val store = createVacanciesStore(
        storeFactory,
        projectId,
        dependencies.vacanciesUseCases,
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

        /** Opens a resume linked to the current vacancy. */
        data class LinkedResume(val resume: Resume) : EditorConfiguration

        /** Opens management of a linked cover letter. */
        data class LinkedLetter(val letter: CoverLetter) : EditorConfiguration
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
        } else if (configuration is EditorConfiguration.LinkedLetter) {
            VacanciesComponent.Child.LinkedLetter(
                DefaultCoverLetterEditorComponent(
                    childContext,
                    configuration.letter,
                    close = { editorNavigation.pop() },
                    deleted = { id ->
                        childStack.value.items.mapNotNull {
                            (it.instance as? VacanciesComponent.Child.Editor)?.component
                        }.lastOrNull()?.letters?.onDeleted(id)
                        editorNavigation.pop()
                    },
                ),
            )
        } else if (configuration is EditorConfiguration.LinkedResume) {
            VacanciesComponent.Child.LinkedResume(
                DefaultResumesComponent(
                    childContext,
                    projectId,
                    back = { editorNavigation.pop() },
                    initialResume = configuration.resume,
                ),
            )
        } else {
            val original = (configuration as? EditorConfiguration.Edit)?.vacancy
            VacanciesComponent.Child.Editor(
                DefaultVacancyEditorComponent(
                    childContext,
                    save = { draft ->
                        val current = editor?.initialVacancy
                        store.accept(
                            if (current == null) {
                                VacanciesIntent.Create(draft)
                            } else {
                                VacanciesIntent.Update(current, draft)
                            },
                        )
                    },
                    initialVacancy = original,
                    projectId = projectId,
                    delete = {
                        editor?.initialVacancy?.let { store.accept(VacanciesIntent.Delete(it.id)) }
                    },
                    close = { if (initialVacancyId != null) back() else editorNavigation.pop() },
                    openResume = { editorNavigation.pushNew(EditorConfiguration.LinkedResume(it)) },
                    openLetter = { editorNavigation.pushNew(EditorConfiguration.LinkedLetter(it)) },
                ),
            )
        }
    }

    override val selection: VacancySelectionComponent = DefaultVacancySelectionComponent(
        componentContext,
        projectId,
        uiState,
        canEdit = { childStack.value.active.instance == VacanciesComponent.Child.List },
    )

    init {
        var openedInitialVacancy = false
        val initialSubscription = uiState.subscribe { state ->
            if (!openedInitialVacancy && initialVacancyId != null &&
                state is VacanciesState.Ready
            ) {
                openedInitialVacancy = true
                state.vacancies.find { it.id == initialVacancyId }?.let(::onEdit)
            }
        }
        lifecycle.doOnDestroy { initialSubscription.cancel() }
        val editorBack = BackCallback(isEnabled = false) { onBack() }
        backHandler.register(editorBack)
        val stackSubscription = childStack.subscribe {
            editorBack.isEnabled =
                it.active.instance != VacanciesComponent.Child.List ||
                selection.state.value.confirming
        }
        val selectionSubscription = selection.state.subscribe {
            editorBack.isEnabled =
                childStack.value.active.instance != VacanciesComponent.Child.List || it.confirming
        }
        lifecycle.doOnDestroy {
            selectionSubscription.cancel()
            stackSubscription.cancel()
            backHandler.unregister(editorBack)
        }
        val subscription = store.labels(
            observer { label ->
                val editor = editor as? DefaultVacancyEditorComponent
                when (label) {
                    VacanciesLabel.Saving -> editor?.saveState?.value = VacancySaveState.Saving

                    is VacanciesLabel.Saved ->
                        editor?.onSaveCompleted(VacancySaveState.Idle, label.createdVacancy)

                    VacanciesLabel.Deleted -> if (initialVacancyId !=
                        null
                    ) {
                        back()
                    } else {
                        editorNavigation.pop()
                    }

                    VacanciesLabel.Deleting ->
                        editor?.deleteState?.value =
                            VacancyDeleteState.Deleting

                    VacanciesLabel.DeleteFailed ->
                        editor?.deleteState?.value =
                            VacancyDeleteState.Failed

                    VacanciesLabel.SaveFailed -> editor?.onSaveCompleted(VacancySaveState.Failed)
                }
            },
        )
        lifecycle.doOnDestroy { subscription.dispose() }
    }

    override fun onAdd() {
        if (listEditable) editorNavigation.pushNew(EditorConfiguration.Create)
    }

    private val listEditable: Boolean get() =
        childStack.value.active.instance == VacanciesComponent.Child.List &&
            !selection.state.value.confirming && !selection.state.value.busy

    override fun onEdit(vacancy: Vacancy) {
        if (listEditable && vacancy.projectId == projectId) {
            editorNavigation.pushNew(EditorConfiguration.Edit(vacancy))
        }
    }

    override fun onRetry() = store.accept(VacanciesIntent.Load)
    override fun onBack() {
        when (val child = childStack.value.active.instance) {
            is VacanciesComponent.Child.Editor -> child.component.onClose()

            is VacanciesComponent.Child.LinkedResume -> child.component.onBack()

            is VacanciesComponent.Child.LinkedLetter -> child.component.onClose()

            VacanciesComponent.Child.List -> {
                if (selection.state.value.confirming) {
                    selection.onCancelDelete()
                } else if (!selection.state.value.busy) {
                    back()
                }
            }
        }
    }
}
