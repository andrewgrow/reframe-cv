package reframecv.ui.components.application.navigation

import reframecv.domain.models.project.Project

data class ProjectTreeState(
    val projects: List<Project> = emptyList(),
    val expandedIds: Set<Long> = emptySet(),
    val selectedId: Long? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
)

data class ProjectTreeRow(val project: Project, val depth: Int, val hasChildren: Boolean)

fun ProjectTreeState.visibleRows(): List<ProjectTreeRow> {
    val children = projects.groupBy { it.parentId }
    val rows = mutableListOf<ProjectTreeRow>()
    val visited = mutableSetOf<Long>()
    fun append(parentId: Long?, depth: Int) {
        children[parentId].orEmpty().forEach { project ->
            if (visited.add(project.id)) {
                val hasChildren = !children[project.id].isNullOrEmpty()
                rows += ProjectTreeRow(project, depth, hasChildren)
                if (project.id in expandedIds) append(project.id, depth + 1)
            }
        }
    }
    append(null, 0)
    return rows
}

fun List<Project>.pathTo(id: Long): List<Project> {
    val byId = associateBy { it.id }
    val path = mutableListOf<Project>()
    val visited = mutableSetOf<Long>()
    var current = byId[id]
    while (current != null && visited.add(current.id)) {
        path += current
        current = current.parentId?.let(byId::get)
    }
    return if (path.lastOrNull()?.parentId == null) path.asReversed() else emptyList()
}
