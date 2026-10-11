package reframecv.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.attachments.TestAttachmentStorage
import reframecv.database.project.ProjectEntity
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.project.ProjectMode
import reframecv.logging.ApplicationLogger

class ProjectDeletionTest : WorkspaceDatabaseTest() {
    @Test
    fun deletesDeepBranchAndDocumentsButKeepsSharedSnapshotsUntilLastReference() = runBlocking {
        val root = project("Root")
        val branch = mutableListOf(root)
        repeat(5) {
            branch += database.projectDao().insertWithParentMode(
                ProjectEntity(
                    name = "Child $it",
                    createdAt = 100,
                    updatedAt = 100,
                    parentId = branch.last(),
                ),
            )
        }
        val leaf = branch.last()
        val other = project("Other")
        val resumeId = database.resumeDao().create(resume(leaf))
        val letterId = database.coverLetterDao().create(letter(leaf))
        database.projectDao().setMainResume(leaf, resumeId, 200)
        val storage = TestAttachmentStorage()
        val snapshots = VacancySnapshotManager(
            database.vacancyDao(),
            database.importRecordDao(),
            storage,
            ApplicationLogger.None,
        )
        val vacancyId = snapshots.create(
            vacancy(leaf).copy(
                resumeId = resumeId,
                coverLetterId = letterId,
            ),
            SnapshotChange.Attach(SelectedAttachment("unused", "source.txt")),
        )
        val source = assertNotNull(snapshots.snapshot(vacancyId)).record
        val shared = database.vacancyDao().create(vacancy(other).copy(importRecordId = source.id))
        val exclusive = snapshots.create(
            vacancy(leaf),
            SnapshotChange.Attach(SelectedAttachment("unused", "exclusive.txt")),
        )
        val exclusiveSource = assertNotNull(snapshots.snapshot(exclusive)).record
        LocalProjectsRepository(database.projectDao(), snapshots).deleteProject(root)
        branch.forEach { assertNull(database.projectDao().findById(it)) }
        assertNull(database.resumeDao().findById(resumeId))
        assertNull(database.coverLetterDao().findById(letterId))
        assertNull(database.vacancyDao().findById(vacancyId))
        assertNull(database.vacancyDao().findById(exclusive))
        assertNull(database.importRecordDao().findById(exclusiveSource.id))
        assertNotNull(database.importRecordDao().findById(source.id))
        assertEquals(setOf(source.filePath), storage.files.keys)
        assertEquals(1, database.vacancyDao().observeCount(other).first())
        snapshots.delete(shared, 500)
        assertNull(database.importRecordDao().findById(source.id))
        assertEquals(emptySet(), storage.files.keys)
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(other)?.mode)
    }

    @Test
    fun deletingLastChildMakesParentConfigurableAgain() = runBlocking {
        val root = project()
        val child = database.projectDao().insertWithParentMode(
            ProjectEntity(name = "Child", parentId = root, createdAt = 100, updatedAt = 100),
        )
        assertEquals(ProjectMode.Container, database.projectDao().findById(root)?.mode)
        database.projectDao().deleteSubtree(child)
        assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(root)?.mode)
        database.resumeDao().create(resume(root))
        assertEquals(ProjectMode.Workspace, database.projectDao().findById(root)?.mode)
    }
}
