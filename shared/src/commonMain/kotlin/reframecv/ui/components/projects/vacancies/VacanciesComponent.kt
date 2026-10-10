package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

interface VacanciesComponent {
    val projectId: Long
    val uiState: Value<VacanciesState>
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
    override fun onRetry() = store.accept(VacanciesIntent.Load)
    override fun onBack() = back()
}
