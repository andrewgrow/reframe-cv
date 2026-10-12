package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.ui.context.AppComponentContext
import reframecv.ui.store.bindStoreToLifecycle

/** Provides a project's resume list, retry action, and return to its dashboard. */
interface ResumesComponent {
    val projectId: Long
    val uiState: Value<ResumesState>
    fun onRetry()
    fun onBack()
}

/** Keeps resume browsing tied to its project and component lifecycle. */
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
    ).also { it.accept(ResumesIntent.Load) }
    override val uiState = bindStoreToLifecycle(store, lifecycle)
    override fun onRetry() = store.accept(ResumesIntent.Load)
    override fun onBack() = back()
}
