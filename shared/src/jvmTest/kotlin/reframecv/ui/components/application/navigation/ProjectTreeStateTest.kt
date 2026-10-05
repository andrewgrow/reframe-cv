package reframecv.ui.components.application.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.project.Project

class ProjectTreeStateTest {
    private val projects = listOf(
        Project(1, "Backend", 0, 0),
        Project(2, "Java", 0, 0, parentId = 1),
        Project(3, "Mobile", 0, 0),
        Project(4, "Android", 0, 0, parentId = 3),
        Project(5, "Google", 0, 0, parentId = 4),
    )

    @Test
    fun showsOnlyExpandedBranchesAndKeepsSiblingOrder() {
        assertEquals(listOf(1L, 3L), ProjectTreeState(projects).visibleRows().map { it.project.id })
        val rows = ProjectTreeState(projects, expandedIds = setOf(3, 4)).visibleRows()
        assertEquals(listOf(1L, 3L, 4L, 5L), rows.map { it.project.id })
        assertEquals(listOf(0, 0, 1, 2), rows.map { it.depth })
        assertEquals(listOf(true, true, true, false), rows.map { it.hasChildren })
        assertEquals(listOf(3L, 4L, 5L), projects.pathTo(5).map { it.id })
    }

    @Test
    fun rejectsMissingParentsAndCyclesWhenBuildingPaths() {
        assertTrue(projects.pathTo(99).isEmpty())
        assertTrue(listOf(projects[1]).pathTo(2).isEmpty())
        val cycle = listOf(projects[0].copy(parentId = 2), projects[1])
        assertTrue(cycle.pathTo(1).isEmpty())
        assertTrue(ProjectTreeState(cycle, expandedIds = setOf(1, 2)).visibleRows().isEmpty())
    }
}
