package reframecv.database.workspace

import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.project.ProjectMode

class WorkspaceModesTest : WorkspaceDatabaseTest() {
    @Test
    fun competingChildAndWorkspaceCreationCannotCommitTogether() = runBlocking {
        val projectId = project()
        val child = async {
            runCatching {
                database.projectDao().insertWithParentMode(
                    ProjectEntity(
                        name = "Child",
                        createdAt = 200,
                        updatedAt = 200,
                        parentId = projectId,
                    ),
                )
            }.isSuccess
        }
        val record =
            async { runCatching { database.resumeDao().create(resume(projectId)) }.isSuccess }
        val childCreated = child.await()
        val recordCreated = record.await()
        assertTrue(childCreated xor recordCreated)
        assertEquals(
            if (childCreated) 1 else 0,
            database.projectDao().observeChildren(projectId).first().size,
        )
        assertEquals(
            if (recordCreated) 1 else 0,
            database.resumeDao().observeCount(projectId).first(),
        )
        assertEquals(
            if (recordCreated) ProjectMode.Workspace else ProjectMode.Container,
            database.projectDao().findById(projectId)?.mode,
        )
    }

    @Test
    fun eachSectionCanStartAWorkspaceAndPreventAddingChildren() = runBlocking {
        val resumeProject = project("Resumes")
        val letterProject = project("Letters")
        val vacancyProject = project("Vacancies")
        database.resumeDao().create(resume(resumeProject))
        database.coverLetterDao().create(letter(letterProject))
        database.vacancyDao().create(vacancy(vacancyProject))
        for (id in listOf(resumeProject, letterProject, vacancyProject)) {
            assertEquals(ProjectMode.Workspace, database.projectDao().findById(id)?.mode)
            assertFailsWith<IllegalStateException> {
                database.projectDao().insertWithParentMode(
                    ProjectEntity(name = "Child", createdAt = 300, updatedAt = 300, parentId = id),
                )
            }
            assertEquals(emptyList(), database.projectDao().observeChildren(id).first())
        }
    }

    @Test
    fun rejectsWorkspaceRecordsInContainersDeletedAndMissingProjects() = runBlocking {
        val container = project()
        database.projectDao().insertWithParentMode(
            ProjectEntity(name = "Child", createdAt = 200, updatedAt = 200, parentId = container),
        )
        val deleted = project("Deleted")
        database.projectDao().markSubtreeDeleted(deleted, 200)
        for (id in listOf(container, deleted, -1L)) {
            assertFailsWith<IllegalStateException> { database.resumeDao().create(resume(id)) }
            assertFailsWith<IllegalStateException> { database.coverLetterDao().create(letter(id)) }
            assertFailsWith<IllegalStateException> { database.vacancyDao().create(vacancy(id)) }
            assertEquals(0, database.resumeDao().observeCount(id).first())
            assertEquals(0, database.coverLetterDao().observeCount(id).first())
            assertEquals(0, database.vacancyDao().observeCount(id).first())
        }
    }

    @Test
    fun failedInsertRollsBackModeAndTimestampInEverySection() = runBlocking {
        val first = project()
        val second = project("Second")
        val resumeId = database.resumeDao().create(resume(first))
        val letterId = database.coverLetterDao().create(letter(first))
        val vacancyId = database.vacancyDao().create(vacancy(first))
        val original = database.projectDao().findById(second)
        assertFailsWith<SQLiteException> {
            database.resumeDao().create(resume(second).copy(id = resumeId))
        }
        assertFailsWith<SQLiteException> {
            database.coverLetterDao().create(letter(second).copy(id = letterId))
        }
        assertFailsWith<SQLiteException> {
            database.vacancyDao().create(vacancy(second).copy(id = vacancyId))
        }
        assertEquals(original, database.projectDao().findById(second))
        assertEquals(emptyList(), database.resumeDao().observeActive(second).first())
        assertEquals(emptyList(), database.coverLetterDao().observeActive(second).first())
        assertEquals(emptyList(), database.vacancyDao().observeActive(second).first())
    }

    @Test
    fun returnsToUnconfiguredOnlyAfterLastRecordIsDeletedAndAllowsChildrenAgain() = runBlocking {
        val projectId = project()
        val resumeId = database.resumeDao().create(resume(projectId))
        val letterId = database.coverLetterDao().create(letter(projectId))
        val vacancyId = database.vacancyDao().create(vacancy(projectId))
        database.resumeDao().softDelete(resumeId, 300)
        assertEquals(ProjectMode.Workspace, database.projectDao().findById(projectId)?.mode)
        database.coverLetterDao().softDelete(letterId, 400)
        assertEquals(ProjectMode.Workspace, database.projectDao().findById(projectId)?.mode)
        assertEquals(1, database.vacancyDao().softDelete(vacancyId, 500))
        assertEquals(0, database.vacancyDao().softDelete(vacancyId, 600))
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(projectId)?.mode)
        assertEquals(500L, database.projectDao().findById(projectId)?.updatedAt)
        database.projectDao().insertWithParentMode(
            ProjectEntity(name = "Child", createdAt = 600, updatedAt = 600, parentId = projectId),
        )
        assertEquals(ProjectMode.Container, database.projectDao().findById(projectId)?.mode)
        assertEquals(emptyList(), database.vacancyDao().search(projectId, "kotlin"))
    }

    @Test
    fun lastResumeOrLetterAlsoResetsMode() = runBlocking {
        val resumeProject = project("Resumes")
        val letterProject = project("Letters")
        val resumeId = database.resumeDao().create(resume(resumeProject))
        val letterId = database.coverLetterDao().create(letter(letterProject))
        database.resumeDao().softDelete(resumeId, 300)
        database.coverLetterDao().softDelete(letterId, 300)
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(resumeProject)?.mode)
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(letterProject)?.mode)
        assertEquals(emptyList(), database.resumeDao().search(resumeProject, "kotlin"))
        assertEquals(emptyList(), database.coverLetterDao().search(letterProject, "kotlin"))
    }

    @Test
    fun deletedProjectsHideAllRecordsAndKeywordsWithoutPhysicallyRemovingThem() = runBlocking {
        val parent = project("Parent")
        val child = database.projectDao().insertWithParentMode(
            ProjectEntity(name = "Child", createdAt = 200, updatedAt = 200, parentId = parent),
        )
        val resumeId = database.resumeDao().create(resume(child))
        val letterId = database.coverLetterDao().create(letter(child))
        val vacancyId = database.vacancyDao().create(vacancy(child))
        database.projectDao().markSubtreeDeleted(parent, 300)
        assertEquals(0, database.resumeDao().observeCount(child).first())
        assertEquals(0, database.coverLetterDao().observeCount(child).first())
        assertEquals(0, database.vacancyDao().observeCount(child).first())
        assertEquals(emptyList(), database.resumeDao().observeActive(child).first())
        assertEquals(emptyList(), database.coverLetterDao().observeActive(child).first())
        assertEquals(emptyList(), database.vacancyDao().observeActive(child).first())
        assertEquals(emptyList(), database.resumeDao().search(child, "kotlin"))
        assertEquals(emptyList(), database.coverLetterDao().search(child, "kotlin"))
        assertEquals(emptyList(), database.vacancyDao().search(child, "kotlin"))
        assertEquals(null, database.resumeDao().findById(resumeId)?.entity?.deletedAt)
        assertEquals(null, database.coverLetterDao().findById(letterId)?.entity?.deletedAt)
        assertEquals(null, database.vacancyDao().findById(vacancyId)?.entity?.deletedAt)
    }
}
