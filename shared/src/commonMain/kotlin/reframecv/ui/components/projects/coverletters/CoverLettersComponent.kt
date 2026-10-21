package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.DefaultVacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

/** Browses project letters, manages a selection, and opens letters or their linked vacancies. */
interface CoverLettersComponent {
    val projectId: Long
    val uiState: Value<CoverLettersState>
    val selection: CoverLetterSelectionComponent
    val childStack: Value<ChildStack<*, Child>>
    val isBusy: Boolean get() = when (val child = childStack.value.active.instance) {
        Child.List -> selection.state.value.deleting
        is Child.Editor -> child.component.state.value.busy
        is Child.LinkedVacancy -> child.component.isBusy
    }

    /** Keeps list selection alive while a document is open. */
    sealed interface Child {
        data object List : Child
        data class Editor(val component: CoverLetterEditorComponent) : Child
        data class LinkedVacancy(val component: VacanciesComponent) : Child
    }
    fun onAdd()
    fun onEdit(letter: CoverLetter)
    fun onOpenVacancy(id: Long)
    fun onRetry()
    fun onBack()
}

/** Coordinates letter browsing and nested document navigation without losing the selection. */
class DefaultCoverLettersComponent(
    context: AppComponentContext,
    override val projectId: Long,
    private val back: () -> Unit,
) : CoverLettersComponent,
    AppComponentContext by context {
    private val store = createCoverLettersStore(
        dependencies.storeFactory,
        projectId,
        dependencies.coverLettersUseCases,
        dependencies.vacanciesUseCases,
        dependencies.logger,
    ).also { it.accept(Unit) }
    override val uiState = bindStoreToLifecycle(store, lifecycle)

    /** Identifies a list or a document opened from it. */
    private sealed interface Configuration {
        data object List : Configuration
        data object Create : Configuration
        data class Edit(val letter: CoverLetter) : Configuration
        data class LinkedVacancy(val vacancy: Vacancy) : Configuration
    }
    private val navigation = StackNavigation<Configuration>()
    override val childStack: Value<ChildStack<*, CoverLettersComponent.Child>> = childStack(
        source = navigation,
        serializer = null,
        initialConfiguration = Configuration.List,
        handleBackButton = false,
    ) { configuration, childContext ->
        when (configuration) {
            Configuration.List -> CoverLettersComponent.Child.List

            is Configuration.Edit, Configuration.Create -> CoverLettersComponent.Child.Editor(
                DefaultCoverLetterEditorComponent(
                    childContext,
                    (configuration as? Configuration.Edit)?.letter ?: CoverLetter(
                        projectId = projectId,
                        name = "",
                        createdAt = 0,
                        updatedAt = 0,
                    ),
                    close = { navigation.pop() },
                    deleted = { navigation.pop() },
                ),
            )

            is Configuration.LinkedVacancy -> CoverLettersComponent.Child.LinkedVacancy(
                DefaultVacanciesComponent(
                    childContext,
                    projectId,
                    back = { navigation.pop() },
                    initialVacancyId = configuration.vacancy.id,
                ),
            )
        }
    }
    override val selection: CoverLetterSelectionComponent = DefaultCoverLetterSelectionComponent(
        context,
        projectId,
        uiState,
        canEdit = { childStack.value.active.instance == CoverLettersComponent.Child.List },
    )
    init {
        val callback = BackCallback(isEnabled = false) { onBack() }
        backHandler.register(callback)
        val stackSubscription = childStack.subscribe {
            callback.isEnabled = it.active.instance != CoverLettersComponent.Child.List ||
                selection.state.value.confirming
        }
        val selectionSubscription = selection.state.subscribe {
            callback.isEnabled =
                childStack.value.active.instance != CoverLettersComponent.Child.List ||
                it.confirming
        }
        lifecycle.doOnDestroy {
            stackSubscription.cancel()
            selectionSubscription.cancel()
            backHandler.unregister(callback)
        }
    }
    override fun onAdd() {
        if (canOpen()) navigation.pushNew(Configuration.Create)
    }
    override fun onEdit(letter: CoverLetter) {
        val ready = uiState.value as? CoverLettersState.Ready ?: return
        if (canOpen()) {
            ready.letters.find { it.id == letter.id }?.let {
                navigation.pushNew(Configuration.Edit(it))
            }
        }
    }
    override fun onOpenVacancy(id: Long) {
        val ready = uiState.value as? CoverLettersState.Ready ?: return
        if (canOpen() && ready.linkedVacancies.any { it.vacancyId == id }) {
            ready.vacancies.find { it.id == id }?.let {
                navigation.pushNew(Configuration.LinkedVacancy(it))
            }
        }
    }
    override fun onRetry() = store.accept(Unit)
    override fun onBack() {
        when (val child = childStack.value.active.instance) {
            is CoverLettersComponent.Child.Editor -> child.component.onClose()

            is CoverLettersComponent.Child.LinkedVacancy -> child.component.onBack()

            CoverLettersComponent.Child.List -> {
                if (selection.state.value.confirming) selection.onCancelDelete() else back()
            }
        }
    }
    private fun canOpen() = childStack.value.active.instance == CoverLettersComponent.Child.List &&
        !selection.state.value.confirming && !selection.state.value.deleting
}
