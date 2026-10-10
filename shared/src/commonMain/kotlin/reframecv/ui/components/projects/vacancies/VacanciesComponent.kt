package reframecv.ui.components.projects.vacancies

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
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

interface VacanciesComponent {
    val projectId: Long
    val uiState: Value<VacanciesState>
    val editorSlot: Value<ChildSlot<*, VacancyEditorComponent>>
    fun onEdit(vacancy: Vacancy)
    fun onAdd()
    fun onRetry()
    fun onBack()
}

class DefaultVacanciesComponent(
    componentContext: AppComponentContext,
    override val projectId: Long,
    private val back: () -> Unit,
    storeFactory: StoreFactory = DefaultStoreFactory(),
) : VacanciesComponent,
    AppComponentContext by componentContext {
    private val store = createVacanciesStore(
        storeFactory,
        projectId,
        dependencies.vacanciesRepository,
    )
        .also { it.accept(VacanciesIntent.Load) }
    override val uiState = bindStoreToLifecycle(store, lifecycle)
    private sealed interface EditorConfiguration {
        data object Create : EditorConfiguration
        data class Edit(val vacancy: Vacancy) : EditorConfiguration
    }

    private val editorNavigation = SlotNavigation<EditorConfiguration>()
    override val editorSlot: Value<ChildSlot<*, VacancyEditorComponent>> = childSlot(
        source = editorNavigation,
        serializer = null,
    ) { configuration, childContext ->
        val original = (configuration as? EditorConfiguration.Edit)?.vacancy
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
            close = { editorNavigation.dismiss() },
        )
    }

    init {
        val editorBack = BackCallback(isEnabled = false) { onBack() }
        backHandler.register(editorBack)
        val slotSubscription = editorSlot.subscribe { editorBack.isEnabled = it.child != null }
        lifecycle.doOnDestroy {
            slotSubscription.cancel()
            backHandler.unregister(editorBack)
        }
        val subscription = store.labels(
            observer { label ->
                val editor = editorSlot.value.child?.instance as? DefaultVacancyEditorComponent
                when (label) {
                    VacanciesLabel.Saving -> editor?.saveState?.value = VacancySaveState.Saving

                    VacanciesLabel.Saved, VacanciesLabel.Deleted -> editorNavigation.dismiss()

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
        if (editorSlot.value.child == null) editorNavigation.activate(EditorConfiguration.Create)
    }

    override fun onEdit(vacancy: Vacancy) {
        if (editorSlot.value.child == null && vacancy.projectId == projectId) {
            editorNavigation.activate(EditorConfiguration.Edit(vacancy))
        }
    }

    override fun onRetry() = store.accept(VacanciesIntent.Load)
    override fun onBack() {
        val editor = editorSlot.value.child?.instance
        if (editor != null) editor.onClose() else back()
    }
}
