package reframecv.ui.components.projects.dashboard

import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

interface DashboardComponent {
    val projectId: Long
    val uiState: Value<DashboardState>
    fun onRetry()
}

class DefaultDashboardComponent(
    componentContext: AppComponentContext,
    override val projectId: Long,
    storeFactory: StoreFactory = DefaultStoreFactory(),
) : DashboardComponent,
    AppComponentContext by componentContext {
    private val store = createDashboardStore(
        storeFactory,
        projectId,
        dependencies.resumesRepository,
        dependencies.vacanciesRepository,
        dependencies.coverLettersRepository,
    ).also { it.accept(DashboardIntent.Load) }
    override val uiState = bindStoreToLifecycle(store, lifecycle)

    override fun onRetry() {
        store.accept(DashboardIntent.Load)
    }
}
