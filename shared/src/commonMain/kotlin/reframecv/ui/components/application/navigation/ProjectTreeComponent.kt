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

interface ProjectTreeComponent {
    val state: Value<ProjectTreeState>
    fun onToggle(id: Long)
    fun onProjectSelected(id: Long)
    fun onProjectsList()
    fun onRetry()
}

class DefaultProjectTreeComponent(
    componentContext: AppComponentContext,
    private val openProject: (List<Project>) -> Unit,
    private val openProjectsList: () -> Unit,
    private val onProjectsChanged: (List<Project>) -> Unit,
) : ProjectTreeComponent {
    override val state = MutableValue(ProjectTreeState())
    private val repository = componentContext.dependencies.projectsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observation: Job? = null

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
        onRetry()
    }

    override fun onRetry() {
        observation?.cancel()
        state.value = state.value.copy(loading = true, failed = false)
        observation = scope.launch {
            try {
                repository.observeAllProjects().collect { projects ->
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
                    onProjectsChanged(projects)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                state.value = state.value.copy(loading = false, failed = true)
            }
        }
    }

    fun select(id: Long?) {
        val ancestors = id?.let { state.value.projects.pathTo(it).dropLast(1) }.orEmpty()
        state.value = state.value.copy(
            selectedId = id,
            expandedIds = state.value.expandedIds + ancestors.map { it.id },
        )
    }

    override fun onToggle(id: Long) {
        val expanded = state.value.expandedIds
        state.value = state.value.copy(
            expandedIds = if (id in expanded) expanded - id else expanded + id,
        )
    }

    override fun onProjectSelected(id: Long) {
        val path = state.value.projects.pathTo(id)
        if (path.isNotEmpty()) openProject(path)
    }

    override fun onProjectsList() = openProjectsList()
}
