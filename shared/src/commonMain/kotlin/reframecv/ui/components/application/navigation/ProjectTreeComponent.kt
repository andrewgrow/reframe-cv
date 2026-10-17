package reframecv.ui.components.application.navigation

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.project.Project
import reframecv.ui.context.AppComponentContext

/** Lets users reach projects directly through the persistent navigation tree. */
interface ProjectTreeComponent {
    val state: Value<ProjectTreeState>
    fun onToggle(id: Long)
    fun onProjectSelected(id: Long)
    fun onProjectsList()
    fun onRetry()
}

/** Keeps tree navigation current and the selected project visible within its ancestry. */
class DefaultProjectTreeComponent(
    componentContext: AppComponentContext,
    private val openProject: (List<Project>) -> Unit,
    private val openProjectsList: () -> Unit,
    private val onProjectsChanged: (List<Project>) -> Unit,
) : ProjectTreeComponent {
    override val state = MutableValue(ProjectTreeState())
    private val history = componentContext.dependencies.eventHistory
    private val logger = componentContext.dependencies.logger
    private val useCases = componentContext.dependencies.projectsUseCases
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null

    init {
        componentContext.lifecycle.doOnDestroy {
            history.record("ProjectTree disposed")
            scope.cancel()
        }
        onRetry()
    }

    // Repository implementations can fail with different exceptions; keep the screen usable.
    @Suppress("TooGenericExceptionCaught")
    override fun onRetry() {
        history.record("ProjectTree Load")
        observation?.cancel()
        state.value = state.value.copy(loading = true, failed = false)
        observation = scope.launch {
            try {
                useCases.observeAll().collect { projects ->
                    val activeIds = projects.map { it.id }.toSet()
                    val ancestors = state.value.selectedId?.let {
                        projects.pathTo(it).dropLast(1).map { project -> project.id }
                    }.orEmpty()
                    state.value = state.value.copy(
                        projects = projects,
                        expandedIds = state.value.expandedIds.intersect(activeIds) + ancestors,
                        loading = false,
                        failed = false,
                    )
                    history.record("ProjectTree Ready(count=${projects.size})")
                    onProjectsChanged(projects)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                history.record("ProjectTree LoadFailed")
                logger.error("Load project tree", error, emptyMap())
                state.value = state.value.copy(loading = false, failed = true)
            }
        }
    }

    fun select(id: Long?) {
        history.record("ProjectTree Selected(projectId=$id)")
        val ancestors = id?.let { state.value.projects.pathTo(it).dropLast(1) }.orEmpty()
        state.value = state.value.copy(
            selectedId = id,
            expandedIds = state.value.expandedIds + ancestors.map { it.id },
        )
    }

    override fun onToggle(id: Long) {
        val expanded = state.value.expandedIds
        history.record("ProjectTree Toggle(projectId=$id, expanded=${id !in expanded})")
        state.value = state.value.copy(
            expandedIds = if (id in expanded) expanded - id else expanded + id,
        )
    }

    override fun onProjectSelected(id: Long) {
        val path = state.value.projects.pathTo(id)
        history.record("ProjectTree OpenProject(projectId=$id, path=${path.map { it.id }})")
        if (path.isNotEmpty()) openProject(path)
    }

    override fun onProjectsList() {
        history.record("ProjectTree OpenProjectsList")
        openProjectsList()
    }
}
