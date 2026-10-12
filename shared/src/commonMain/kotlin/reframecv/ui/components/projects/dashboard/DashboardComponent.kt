package reframecv.ui.components.projects.dashboard

import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

/** Lets the dashboard show workspace documents and open their sections. */
interface DashboardComponent {
    val projectId: Long
    val uiState: Value<DashboardState>
    fun onOpenResumes()
    fun onOpenVacancies()
    fun onBack()
    fun onRetry()
}

/** Keeps the dashboard current while its screen is active and handles leaving the workspace. */
class DefaultDashboardComponent(
    componentContext: AppComponentContext,
    override val projectId: Long,
    storeFactory: StoreFactory = componentContext.dependencies.storeFactory,
    private val openVacancies: () -> Unit = {},
    private val openResumes: () -> Unit = {},
    private val back: () -> Unit = {},
) : DashboardComponent,
    AppComponentContext by componentContext {
    private val store = createDashboardStore(
        storeFactory,
        projectId,
        dependencies.resumesRepository,
        dependencies.vacanciesRepository,
        dependencies.coverLettersRepository,
        dependencies.logger,
    ).also { it.accept(DashboardIntent.Load) }
    override val uiState = bindStoreToLifecycle(store, lifecycle)

    init {
        val callback = BackCallback { onBack() }
        backHandler.register(callback)
        lifecycle.doOnDestroy { backHandler.unregister(callback) }
    }

    override fun onBack() = back()

    override fun onOpenResumes() = openResumes()

    override fun onOpenVacancies() = openVacancies()

    override fun onRetry() {
        store.accept(DashboardIntent.Load)
    }
}
